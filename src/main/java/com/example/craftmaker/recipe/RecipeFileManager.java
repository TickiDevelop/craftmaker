package com.example.craftmaker.recipe;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.loading.FMLPaths;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Управляет файлами рецептов в папке crafts/.
 * Хранит все рецепты в одной папке без подпапок.
 */
public class RecipeFileManager {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    private static Path craftsDir;

    /**
     * Создаёт папку crafts/ если она не существует.
     */
    public static void ensureFolders() {
        Path gameDir = FMLPaths.GAMEDIR.get();
        craftsDir = gameDir.resolve("crafts");
        
        try {
            if (!Files.exists(craftsDir)) {
                Files.createDirectories(craftsDir);
                LOGGER.info("[CraftMaker] Created crafts directory: {}", craftsDir);
            }
        } catch (IOException e) {
            LOGGER.error("[CraftMaker] Failed to create crafts directory", e);
        }
    }

    /**
     * Сохраняет рецепт в папку crafts/.
     */
    public static void saveRecipe(String filename, JsonObject json) {
        ensureFolders();
        
        if (!filename.endsWith(".json")) {
            filename = filename + ".json";
        }
        
        Path filePath = craftsDir.resolve(filename);
        
        try {
            Files.writeString(filePath, GSON.toJson(json), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            LOGGER.info("[CraftMaker] Saved recipe: {}", filename);
        } catch (IOException e) {
            LOGGER.error("[CraftMaker] Failed to save recipe: {}", filename, e);
        }
    }

    /**
     * Загружает рецепт из папки crafts/.
     */
    public static JsonObject loadRecipe(String filename) {
        ensureFolders();
        
        if (!filename.endsWith(".json")) {
            filename = filename + ".json";
        }
        
        Path filePath = craftsDir.resolve(filename);
        
        try {
            if (!Files.exists(filePath)) {
                return null;
            }
            
            String content = Files.readString(filePath);
            return GSON.fromJson(content, JsonObject.class);
        } catch (IOException e) {
            LOGGER.error("[CraftMaker] Failed to load recipe: {}", filename, e);
            return null;
        }
    }

    /**
     * Удаляет рецепт из папки crafts/.
     */
    public static void deleteRecipe(String filename) {
        ensureFolders();
        
        if (!filename.endsWith(".json")) {
            filename = filename + ".json";
        }
        
        Path filePath = craftsDir.resolve(filename);
        
        try {
            Files.deleteIfExists(filePath);
            LOGGER.info("[CraftMaker] Deleted recipe: {}", filename);
        } catch (IOException e) {
            LOGGER.error("[CraftMaker] Failed to delete recipe: {}", filename, e);
        }
    }

    /**
     * Загружает все рецепты из папки crafts/.
     */
    public static List<RecipeFile> loadAllRecipes() {
        ensureFolders();
        
        List<RecipeFile> recipes = new ArrayList<>();
        
        try (Stream<Path> paths = Files.walk(craftsDir)) {
            paths.filter(Files::isRegularFile)
                 .filter(p -> p.toString().endsWith(".json"))
                 .forEach(p -> {
                     try {
                         String content = Files.readString(p);
                         JsonObject json = GSON.fromJson(content, JsonObject.class);
                         if (json != null) {
                             String filename = p.getFileName().toString();
                             recipes.add(new RecipeFile(filename, json));
                         }
                     } catch (IOException e) {
                         LOGGER.error("[CraftMaker] Failed to load recipe from: {}", p, e);
                     }
                 });
        } catch (IOException e) {
            LOGGER.error("[CraftMaker] Failed to scan crafts directory", e);
        }
        
        return recipes;
    }

    /**
     * Получает путь к папке crafts/.
     */
    public static Path getCraftsDir() {
        ensureFolders();
        return craftsDir;
    }

    /**
     * Строит корректный JSON shaped-рецепта из 3x3 сетки ингредиентов (слоты 0-8) и результата (слот 9).
     * Каждый уникальный тип предмета получает свою букву; паттерн и ключ используют одни и те же буквы.
     *
     * @param container Container с 10 слотами: 0-8 — ингредиенты, 9 — результат
     * @return JSON-объект в формате minecraft:crafting_shaped
     */
    public static JsonObject buildShapedRecipeJson(Object container) {
        // Создаем JSON для shaped рецепта
        JsonObject json = new JsonObject();
        json.addProperty("type", "minecraft:crafting_shaped");
        
        try {
            // Получаем предметы из контейнера через рефлексию
            ItemStack[] items = new ItemStack[10];
            java.lang.reflect.Field itemsField = null;
            for (java.lang.reflect.Field f : container.getClass().getDeclaredFields()) {
                if (f.getType().isArray() && f.getType().getComponentType() == ItemStack.class) {
                    itemsField = f;
                    break;
                }
            }
            
            if (itemsField != null) {
                itemsField.setAccessible(true);
                Object containerItems = itemsField.get(container);
                if (containerItems instanceof ItemStack[]) {
                    items = (ItemStack[]) containerItems;
                }
            }
            
            // Если не удалось получить через рефлексию, пробуем через getItem
            if (itemsField == null) {
                for (int i = 0; i < 10; i++) {
                    try {
                        java.lang.reflect.Method getItemMethod = container.getClass().getMethod("getItem", int.class);
                        ItemStack stack = (ItemStack) getItemMethod.invoke(container, i);
                        items[i] = stack;
                    } catch (Exception e) {
                        items[i] = ItemStack.EMPTY;
                    }
                }
            }
            
            // Создаем pattern и key
            char nextChar = 'A';
            Map<Character, String> charToItem = new LinkedHashMap<>();
            JsonArray patternArray = new JsonArray();
            JsonObject key = new JsonObject();
            
            for (int row = 0; row < 3; row++) {
                StringBuilder patternRow = new StringBuilder();
                for (int col = 0; col < 3; col++) {
                    ItemStack stack = items[row * 3 + col];
                    if (stack != null && !stack.isEmpty()) {
                        String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
                        if (!charToItem.containsValue(itemId)) {
                            charToItem.put(nextChar, itemId);
                            patternRow.append(nextChar);
                            nextChar++;
                        } else {
                            // Находим существующую букву для этого предмета
                            for (Map.Entry<Character, String> entry : charToItem.entrySet()) {
                                if (entry.getValue().equals(itemId)) {
                                    patternRow.append(entry.getKey());
                                    break;
                                }
                            }
                        }
                    } else {
                        patternRow.append(' ');
                    }
                }
                patternArray.add(patternRow.toString());
            }
            
            // Заполняем key
            for (Map.Entry<Character, String> entry : charToItem.entrySet()) {
                JsonObject itemObj = new JsonObject();
                itemObj.addProperty("item", entry.getValue());
                key.add(String.valueOf(entry.getKey()), itemObj);
            }
            
            json.add("pattern", patternArray);
            json.add("key", key);
            
            // Результат
            ItemStack resultStack = items[9];
            JsonObject result = new JsonObject();
            if (resultStack != null && !resultStack.isEmpty()) {
                String resultId = BuiltInRegistries.ITEM.getKey(resultStack.getItem()).toString();
                result.addProperty("id", resultId);
                result.addProperty("count", resultStack.getCount());
            } else {
                result.addProperty("id", "minecraft:air");
                result.addProperty("count", 1);
            }
            json.add("result", result);
            
        } catch (Exception e) {
            LOGGER.error("[CraftMaker] Failed to build recipe JSON from container", e);
            // Возвращаем пустой шаблон при ошибке
            JsonArray patternArray = new JsonArray();
            patternArray.add("   ");
            patternArray.add("   ");
            patternArray.add("   ");
            json.add("pattern", patternArray);
            json.add("key", new JsonObject());
            JsonObject result = new JsonObject();
            result.addProperty("id", "minecraft:air");
            result.addProperty("count", 1);
            json.add("result", result);
        }
        
        return json;
    }

    /**
     * Класс для хранения пары (имя файла, JSON).
     */
    public static class RecipeFile {
        public final String filename;
        public final JsonObject json;
        
        public RecipeFile(String filename, JsonObject json) {
            this.filename = filename;
            this.json = json;
        }
    }
}
