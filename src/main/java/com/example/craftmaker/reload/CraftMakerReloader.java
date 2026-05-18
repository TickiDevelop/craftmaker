package com.example.craftmaker.reload;

import com.example.craftmaker.recipe.RecipeFileManager;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeMap;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Кастомная система рецептов CraftMaker.
 * Инжектирует рецепты напрямую в RecipeManager через рефлексию.
 * Поддерживает shaped и shapeless крафт, удаление рецептов.
 *
 * API-примечания для MC 1.21.11:
 *  - ResourceLocation переименован в Identifier (net.minecraft.resources.Identifier)
 *  - RecipeHolder использует ResourceKey<Recipe<?>> как ID
 *  - RecipeMap.create(Iterable) — фабричный метод
 *  - RecipeManager.recipeMap() — NeoForge-добавленный метод для доступа к карте
 *  - ClientboundUpdateRecipesPacket больше не переносит крафтовые рецепты (не используем)
 */
public class CraftMakerReloader {
    private static final Logger LOGGER = LogManager.getLogger();

    /** ResourceKey рецептов, инжектированных нами — удаляем при следующем reload */
    private static final Set<ResourceKey<Recipe<?>>> injectedKeys = new LinkedHashSet<>();

    /**
     * Полная перезагрузка: чистит старые инъекции, применяет remove/, add/.
     * Сервер-сторонняя операция; клиент увидит результат при следующем крафте.
     */
    public static void reload() {
        LOGGER.info("[CraftMaker] Starting recipe reload...");

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            LOGGER.warn("[CraftMaker] Server not available, skipping reload");
            return;
        }

        RecipeManager manager = server.getRecipeManager();

        // Получаем текущую карту через NeoForge-добавленный метод recipeMap()
        RecipeMap currentMap = manager.recipeMap();

        // Строим изменяемый список всех текущих рецептов
        List<RecipeHolder<?>> allRecipes = new ArrayList<>(currentMap.values());
        // Шаг 1 — убираем рецепты, которые мы инжектировали прошлый раз
        allRecipes.removeIf(h -> injectedKeys.contains(h.id()));
        injectedKeys.clear();
        LOGGER.info("[CraftMaker] Cleared previous injections");

        // Загружаем рецепты для добавления из папки crafts/
        int added = 0;
        List<RecipeFileManager.RecipeFile> addRecipes = RecipeFileManager.loadAllRecipes();
        
        // Сначала собираем все рецепты CraftMaker
        List<RecipeHolder<?>> craftMakerRecipes = new ArrayList<>();
        for (RecipeFileManager.RecipeFile rf : addRecipes) {
            try {
                RecipeHolder<?> holder = parseRecipeHolder(rf);
                if (holder != null) {
                    craftMakerRecipes.add(holder);
                    injectedKeys.add(holder.id());
                    added++;
                    LOGGER.info("[CraftMaker] Loaded recipe: {}", holder.id());
                }
            } catch (Exception e) {
                LOGGER.error("[CraftMaker] Failed to parse {}: {}", rf.filename, e.getMessage());
            }
        }
        
        // Добавляем рецепты CraftMaker в НАЧАЛО списка для приоритета
        // Это гарантирует что рецепты CraftMaker будут проверены первыми
        craftMakerRecipes.addAll(allRecipes);
        allRecipes = craftMakerRecipes;

        // Создаём новую RecipeMap через фабричный метод и инжектируем через рефлексию
        RecipeMap newMap = RecipeMap.create(allRecipes);
        setRecipeMapReflection(manager, newMap);

        LOGGER.info("[CraftMaker] Reload done. Injected: {}", added);
    }

    // ---- Рефлексия для замены RecipeMap в RecipeManager ----

    private static void setRecipeMapReflection(RecipeManager manager, RecipeMap map) {
        try {
            Field f = findFieldByType(RecipeManager.class, RecipeMap.class);
            if (f != null) {
                f.setAccessible(true);
                f.set(manager, map);
                LOGGER.info("[CraftMaker] RecipeMap set via reflection");
            } else {
                LOGGER.error("[CraftMaker] RecipeMap field not found in RecipeManager");
            }
        } catch (Exception e) {
            LOGGER.error("[CraftMaker] setRecipeMap reflection error", e);
        }
    }

    /** Ищет поле заданного типа во всей иерархии классов (не зависит от имени поля) */
    private static Field findFieldByType(Class<?> cls, Class<?> type) {
        for (Class<?> c = cls; c != null; c = c.getSuperclass()) {
            for (Field f : c.getDeclaredFields()) {
                if (f.getType() == type) return f;
            }
        }
        return null;
    }

    // ---- Парсинг рецептов ----

    private static RecipeHolder<?> parseRecipeHolder(RecipeFileManager.RecipeFile rf) {
        JsonObject json = rf.json;
        String type = json.has("type") ? json.get("type").getAsString() : "minecraft:crafting_shaped";

        // ID рецепта — craftmaker:<имя_файла_без_расширения>
        Identifier idLoc = Identifier.fromNamespaceAndPath("craftmaker",
                rf.filename.replace(".json", "").toLowerCase(Locale.ROOT));
        ResourceKey<Recipe<?>> key = ResourceKey.create(Registries.RECIPE, idLoc);

        return switch (type) {
            case "minecraft:crafting_shaped" -> {
                ShapedRecipe r = buildShaped(json);
                yield r != null ? new RecipeHolder<>(key, r) : null;
            }
            case "minecraft:crafting_shapeless" -> {
                ShapelessRecipe r = buildShapeless(json);
                yield r != null ? new RecipeHolder<>(key, r) : null;
            }
            default -> {
                LOGGER.warn("[CraftMaker] Unsupported recipe type '{}' in {}", type, rf.filename);
                yield null;
            }
        };
    }

    /** Строит ShapedRecipe из JSON объекта */
    private static ShapedRecipe buildShaped(JsonObject json) {
        JsonArray patternArr = json.getAsJsonArray("pattern");
        String[] pattern = new String[patternArr.size()];
        for (int i = 0; i < patternArr.size(); i++) pattern[i] = patternArr.get(i).getAsString();

        Map<Character, Ingredient> keyMap = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> e : json.getAsJsonObject("key").entrySet()) {
            Item item = resolveItem(e.getValue().getAsJsonObject().get("item").getAsString());
            if (item == null) return null;
            keyMap.put(e.getKey().charAt(0), Ingredient.of(item));
        }

        ItemStack result = resolveResult(json.getAsJsonObject("result"));
        if (result == null) return null;

        return new ShapedRecipe("craftmaker", CraftingBookCategory.MISC,
                ShapedRecipePattern.of(keyMap, pattern), result);
    }

    /** Строит ShapelessRecipe из JSON объекта */
    private static ShapelessRecipe buildShapeless(JsonObject json) {
        List<Ingredient> ingredients = new ArrayList<>();
        for (JsonElement elem : json.getAsJsonArray("ingredients")) {
            Item item = resolveItem(elem.getAsJsonObject().get("item").getAsString());
            if (item == null) return null;
            ingredients.add(Ingredient.of(item));
        }

        ItemStack result = resolveResult(json.getAsJsonObject("result"));
        if (result == null) return null;

        return new ShapelessRecipe("craftmaker", CraftingBookCategory.MISC, result, ingredients);
    }

    /** Находит Item по строке ID ("minecraft:stone"). Возвращает null если не найден. */
    private static Item resolveItem(String idStr) {
        Identifier loc = Identifier.tryParse(idStr);
        if (loc == null) {
            LOGGER.error("[CraftMaker] Invalid item ID: {}", idStr);
            return null;
        }
        // getValue() возвращает дефолтный предмет (AIR) если не найден
        Item item = BuiltInRegistries.ITEM.getValue(loc);
        if (item == null || (item == net.minecraft.world.item.Items.AIR
                && !idStr.equals("minecraft:air"))) {
            LOGGER.error("[CraftMaker] Unknown item: {}", idStr);
            return null;
        }
        return item;
    }

    /** Парсит result-объект, поддерживает "id" и "item" для совместимости */
    private static ItemStack resolveResult(JsonObject obj) {
        if (obj == null) return null;
        String idStr = obj.has("id") ? obj.get("id").getAsString()
                     : obj.has("item") ? obj.get("item").getAsString() : null;
        if (idStr == null) { LOGGER.error("[CraftMaker] Result has no 'id'/'item' field"); return null; }
        Item item = resolveItem(idStr);
        if (item == null) return null;
        int count = obj.has("count") ? obj.get("count").getAsInt() : 1;
        return new ItemStack(item, count);
    }

    /** Возвращает множество ключей рецептов, инжектированных в данный момент */
    public static Set<ResourceKey<Recipe<?>>> getInjectedKeys() {
        return Collections.unmodifiableSet(injectedKeys);
    }
}
