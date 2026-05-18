package com.example.craftmaker.client.gui;

import com.example.craftmaker.inventory.RecipeContainer;
import com.example.craftmaker.network.CraftMakerPackets;
import com.example.craftmaker.recipe.RecipeFileManager;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;

/**
 * Кастомный экран создания рецептов без AbstractContainerScreen.
 * Использует только базовый Java для перетаскивания предметов.
 */
public class AddRecipeCustomScreen extends Screen {
    
    private final Inventory playerInventory;
    private final RecipeContainer recipeContainer = new RecipeContainer();
    private final Screen parent;
    private final boolean editMode;
    private final String originalFilename;
    private EditBox recipeNameField;
    private String recipeName = "recipe_" + System.currentTimeMillis();
    
    // Перетаскивание
    private ItemStack carriedItem = ItemStack.EMPTY;
    private boolean isFromRecipe = false;
    
    // Защита от многократного нажатия кнопки
    private long lastMouseClickTime = 0;
    private static final long CLICK_COOLDOWN = 100; // мс между кликами
    
    // Позиции слотов для обработки кликов
    private int centerX;
    private int centerY;
    
    public int getCenterX() {
        return centerX;
    }
    
    public int getCenterY() {
        return centerY;
    }
    
    public AddRecipeCustomScreen(Inventory playerInventory, Screen parent) {
        super(Component.literal("Create Recipe"));
        this.playerInventory = playerInventory;
        this.parent = parent;
        this.editMode = false;
        this.originalFilename = null;
    }

    public AddRecipeCustomScreen(Inventory playerInventory, Screen parent, RecipeFileManager.RecipeFile recipeFile) {
        super(Component.literal("Edit Recipe"));
        this.playerInventory = playerInventory;
        this.parent = parent;
        this.editMode = true;
        this.originalFilename = recipeFile.filename;
        this.recipeName = recipeFile.filename.endsWith(".json")
                ? recipeFile.filename.substring(0, recipeFile.filename.length() - 5)
                : recipeFile.filename;
        loadRecipe(recipeFile.json);
    }
    
    @Override
    protected void init() {
        recipeNameField = new EditBox(this.font, this.width / 2 - 80, this.height / 2 - 125, 160, 20, Component.literal("Recipe Name"));
        recipeNameField.setMaxLength(64);
        recipeNameField.setValue(recipeName);
        recipeNameField.setResponder(value -> recipeName = value);
        this.addRenderableWidget(recipeNameField);

        // Кнопка "Save" над слотами крафта
        this.addRenderableWidget(Button.builder(Component.literal(editMode ? "Save Changes" : "Save"), button -> {
            saveRecipe();
        }).bounds(this.width / 2 - 80, this.height / 2 - 100, 160, 20).build());
        
        // Кнопка "Back" ниже хотбара
        this.addRenderableWidget(Button.builder(Component.literal("Back"), button -> {
            this.minecraft.setScreen(parent);
        }).bounds(this.width / 2 - 80, this.height / 2 + 90, 160, 20).build());
    }
    
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFF);
        graphics.drawString(this.font, Component.literal("Recipe Name"), this.width / 2 - 80, this.height / 2 - 138, 0xFFFFFF);
        
        centerX = this.width / 2;
        centerY = this.height / 2 - 40; // Поднят на 20% вверх
        
        // Рисуем слоты ингредиентов 3x3
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int slotX = centerX - 27 + col * 18;
                int slotY = centerY - 27 + row * 18;
                drawSlot(graphics, slotX, slotY, recipeContainer.getItem(row * 3 + col), mouseX, mouseY);
            }
        }
        
        // Рисуем слот результата на уровне среднего ряда, с отступом вправо
        drawSlot(graphics, centerX + 35, centerY - 9, recipeContainer.getItem(9), mouseX, mouseY);
        
        // Рисуем инвентарь игрока
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int slotX = centerX - 81 + col * 18;
                int slotY = centerY + 40 + row * 18;
                drawSlot(graphics, slotX, slotY, playerInventory.getItem(col + row * 9 + 9), mouseX, mouseY);
            }
        }
        
        // Хотбар
        for (int col = 0; col < 9; col++) {
            int slotX = centerX - 81 + col * 18;
            int slotY = centerY + 100;
            drawSlot(graphics, slotX, slotY, playerInventory.getItem(col), mouseX, mouseY);
        }
        
        // Рисуем перетаскиваемый предмет
        if (!carriedItem.isEmpty()) {
            graphics.renderItem(carriedItem, mouseX - 8, mouseY - 8);
            graphics.renderItemDecorations(this.font, carriedItem, mouseX - 8, mouseY - 8);
        }
        
        super.render(graphics, mouseX, mouseY, partialTick);
    }
    
    private void drawSlot(GuiGraphics graphics, int x, int y, ItemStack stack, int mouseX, int mouseY) {
        // Фон слота — только внешний контур
        graphics.fill(x, y, x + 16, y + 16, 0xFF8B8B8B);
        // Внутренняя часть — тёмный фон
        graphics.fill(x + 1, y + 1, x + 15, y + 15, 0xFF3C3C3C);
        
        // Подсветка при наведении
        if (mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16) {
            graphics.fill(x + 1, y + 1, x + 15, y + 15, 0xFF808080);
        }
        
        // Предмет
        if (!stack.isEmpty()) {
            graphics.renderItem(stack, x, y);
            graphics.renderItemDecorations(this.font, stack, x, y);
        }
    }
    
    private void saveRecipe() {
        // Проверяем слот результата перед генерацией JSON
        if (recipeContainer.getItem(9).isEmpty()) {
            this.minecraft.player.displayClientMessage(Component.literal("Error: result slot is empty!"), false);
            return;
        }

        // Корректный JSON с согласованными pattern + key (одна уникальная буква на тип предмета)
        JsonObject recipeJson = RecipeFileManager.buildShapedRecipeJson(recipeContainer);

        String filename = sanitizeFilename(recipeNameField.getValue().trim());
        if (filename.isEmpty()) {
            this.minecraft.player.displayClientMessage(Component.literal("Error: recipe name is empty!"), false);
            return;
        }

        // Проверка дубликатов
        if (!editMode) {
            // Проверяем существует ли файл
            if (RecipeFileManager.loadRecipe(filename + ".json") != null) {
                this.minecraft.setScreen(new ConfirmDialogScreen(this, "Recipe with this name already exists, enter another name"));
                return;
            }
        } else {
            if (originalFilename != null && !originalFilename.equals(filename + ".json")) {
                if (RecipeFileManager.loadRecipe(filename + ".json") != null) {
                    this.minecraft.setScreen(new ConfirmDialogScreen(this, "Recipe with this name already exists, enter another name"));
                    return;
                }
            }
        }

        try {
            // Проверяем, находимся ли мы на сервере
            boolean isRemoteServer = this.minecraft.player != null && 
                                  this.minecraft.player.connection != null && 
                                  !this.minecraft.isSingleplayer();
            
            if (isRemoteServer) {
                // Отправляем рецепт на сервер через сетевой пакет
                String jsonStr = new com.google.gson.Gson().toJson(recipeJson);
                CraftMakerPackets.sendSaveRecipeToServer(filename, jsonStr);
                this.minecraft.player.displayClientMessage(Component.literal("Recipe sent to server: " + filename), false);
            } else {
                // Локальное сохранение для одиночной игры
                if (editMode && originalFilename != null && !originalFilename.equals(filename + ".json")) {
                    RecipeFileManager.deleteRecipe(originalFilename);
                }
                RecipeFileManager.saveRecipe(filename, recipeJson);
                this.minecraft.player.displayClientMessage(Component.literal((editMode ? "Recipe updated: " : "Recipe saved: ") + filename), false);
                com.example.craftmaker.reload.CraftMakerReloader.reload();
            }
            
            this.onClose();
        } catch (Exception e) {
            this.minecraft.player.displayClientMessage(Component.literal("Saving error: " + e.getMessage()), false);
            e.printStackTrace();
        }
    }

    private void loadRecipe(JsonObject json) {
        if (!json.has("pattern") || !json.has("key")) {
            return;
        }

        Map<Character, ItemStack> keyStacks = new HashMap<>();
        JsonObject key = json.getAsJsonObject("key");
        for (Map.Entry<String, JsonElement> entry : key.entrySet()) {
            if (entry.getKey().isEmpty()) {
                continue;
            }

            JsonObject ingredient = entry.getValue().getAsJsonObject();
            if (!ingredient.has("item")) {
                continue;
            }

            ItemStack stack = stackFromItemId(ingredient.get("item").getAsString(), 1);
            if (!stack.isEmpty()) {
                keyStacks.put(entry.getKey().charAt(0), stack);
            }
        }

        var pattern = json.getAsJsonArray("pattern");
        for (int row = 0; row < Math.min(3, pattern.size()); row++) {
            String line = pattern.get(row).getAsString();
            for (int col = 0; col < Math.min(3, line.length()); col++) {
                char symbol = line.charAt(col);
                if (symbol != ' ' && keyStacks.containsKey(symbol)) {
                    recipeContainer.setItem(row * 3 + col, keyStacks.get(symbol).copy());
                }
            }
        }

        if (json.has("result")) {
            JsonObject result = json.getAsJsonObject("result");
            String itemId = result.has("id") ? result.get("id").getAsString()
                    : result.has("item") ? result.get("item").getAsString() : "";
            int count = result.has("count") ? result.get("count").getAsInt() : 1;
            recipeContainer.setItem(9, stackFromItemId(itemId, count));
        }
    }

    private ItemStack stackFromItemId(String itemId, int count) {
        Identifier id = Identifier.tryParse(itemId);
        if (id == null) {
            return ItemStack.EMPTY;
        }

        Item item = BuiltInRegistries.ITEM.getValue(id);
        if (item == null || item == net.minecraft.world.item.Items.AIR && !"minecraft:air".equals(itemId)) {
            return ItemStack.EMPTY;
        }

        return new ItemStack(item, Math.max(1, count));
    }

    private String sanitizeFilename(String value) {
        return value.toLowerCase(java.util.Locale.ROOT)
                .replaceAll("[^a-z0-9_\\-]", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_+|_+$", "");
    }
    
    @Override
    public void onClose() {
        // Возвращаем перетаскиваемый предмет
        if (!carriedItem.isEmpty()) {
            if (isFromRecipe) {
                // Ищем пустой слот в рецепте
                for (int i = 0; i < 9; i++) {
                    if (recipeContainer.getItem(i).isEmpty()) {
                        recipeContainer.setItem(i, carriedItem);
                        carriedItem = ItemStack.EMPTY;
                        break;
                    }
                }
            } else {
                // Ищем пустой слот в инвентаре
                for (int i = 0; i < playerInventory.getContainerSize(); i++) {
                    if (playerInventory.getItem(i).isEmpty()) {
                        playerInventory.setItem(i, carriedItem);
                        carriedItem = ItemStack.EMPTY;
                        break;
                    }
                }
            }
            // Если не нашли пустой слот, дропаем предмет
            if (!carriedItem.isEmpty()) {
                // Нельзя дропать предмет на клиенте без сервера
                carriedItem = ItemStack.EMPTY;
            }
        }
        super.onClose();
    }
    
    @Override
    public void removed() {
        super.removed();
    }
    
    @Override
    public boolean isPauseScreen() {
        return true;
    }
    
    public boolean handleMouseClick(int mx, int my, int button) {
        // Защита от многократного нажатия кнопки
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastMouseClickTime < CLICK_COOLDOWN) {
            return false;
        }
        lastMouseClickTime = currentTime;
        
        // Проверяем клик по слотам ингредиентов 3x3
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int slotX = centerX - 27 + col * 18;
                int slotY = centerY - 27 + row * 18;
                if (mx >= slotX && mx < slotX + 16 && my >= slotY && my < slotY + 16) {
                    handleSlotClick(row * 3 + col, true, button);
                    return true;
                }
            }
        }
        
        // Проверяем клик по слоту результата
        int resultSlotX = centerX + 35;
        int resultSlotY = centerY - 9;
        if (mx >= resultSlotX && mx < resultSlotX + 16 && my >= resultSlotY && my < resultSlotY + 16) {
            handleSlotClick(9, true, button);
            return true;
        }
        
        // Проверяем клик по инвентарю игрока
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int slotX = centerX - 81 + col * 18;
                int slotY = centerY + 40 + row * 18;
                if (mx >= slotX && mx < slotX + 16 && my >= slotY && my < slotY + 16) {
                    handleSlotClick(col + row * 9 + 9, false, button);
                    return true;
                }
            }
        }
        
        // Проверяем клик по хотбару
        for (int col = 0; col < 9; col++) {
            int slotX = centerX - 81 + col * 18;
            int slotY = centerY + 100;
            if (mx >= slotX && mx < slotX + 16 && my >= slotY && my < slotY + 16) {
                handleSlotClick(col, false, button);
                return true;
            }
        }
        
        // Если клик вне слотов и есть перетаскиваемый предмет, возвращаем его
        if (!carriedItem.isEmpty()) {
            returnItem();
            return true;
        }
        
        return false;
    }
    
    private void handleSlotClick(int slotIndex, boolean isRecipeSlot, int button) {
        ItemStack slotItem = isRecipeSlot ? recipeContainer.getItem(slotIndex) : playerInventory.getItem(slotIndex);
        
        if (carriedItem.isEmpty()) {
            // Берем предмет из слота
            if (!slotItem.isEmpty()) {
                if (button == 0) { // Левый клик - берем весь стак
                    carriedItem = slotItem.copy();
                    if (!isRecipeSlot) {
                        playerInventory.setItem(slotIndex, ItemStack.EMPTY);
                    } else {
                        recipeContainer.setItem(slotIndex, ItemStack.EMPTY);
                    }
                    isFromRecipe = isRecipeSlot;
                } else if (button == 1) { // Правый клик - берем половину стака
                    int halfCount = (slotItem.getCount() + 1) / 2;
                    carriedItem = slotItem.copy();
                    carriedItem.setCount(halfCount);
                    slotItem.setCount(slotItem.getCount() - halfCount);
                    if (slotItem.isEmpty()) {
                        if (!isRecipeSlot) {
                            playerInventory.setItem(slotIndex, ItemStack.EMPTY);
                        } else {
                            recipeContainer.setItem(slotIndex, ItemStack.EMPTY);
                        }
                    }
                    isFromRecipe = isRecipeSlot;
                } else if (button == 2 && this.minecraft.player.getAbilities().instabuild) { // Средний клик - копируем стак (только в креативе)
                    carriedItem = slotItem.copy();
                    isFromRecipe = isRecipeSlot;
                }
            }
        } else {
            // Кладем предмет в слот
            if (slotItem.isEmpty()) {
                // Слот пустой
                if (button == 0) { // Левый клик - кладем весь предмет
                    if (!isRecipeSlot) {
                        playerInventory.setItem(slotIndex, carriedItem.copy());
                    } else {
                        recipeContainer.setItem(slotIndex, carriedItem.copy());
                    }
                    carriedItem = ItemStack.EMPTY;
                } else if (button == 1) { // Правый клик - кладем один предмет
                    ItemStack oneItem = carriedItem.copy();
                    oneItem.setCount(1);
                    carriedItem.setCount(carriedItem.getCount() - 1);
                    if (carriedItem.isEmpty()) {
                        carriedItem = ItemStack.EMPTY;
                    }
                    if (!isRecipeSlot) {
                        playerInventory.setItem(slotIndex, oneItem);
                    } else {
                        recipeContainer.setItem(slotIndex, oneItem);
                    }
                }
            } else {
                // Слот не пустой
                if (ItemStack.isSameItemSameComponents(slotItem, carriedItem)) {
                    // Предметы одинаковые
                    if (button == 0) { // Левый клик - объединяем
                        int newCount = slotItem.getCount() + carriedItem.getCount();
                        int maxCount = slotItem.getMaxStackSize();
                        if (newCount <= maxCount) {
                            slotItem.setCount(newCount);
                            carriedItem = ItemStack.EMPTY;
                        } else {
                            slotItem.setCount(maxCount);
                            carriedItem.setCount(newCount - maxCount);
                        }
                    } else if (button == 1) { // Правый клик - кладем один предмет
                        if (slotItem.getCount() < slotItem.getMaxStackSize()) {
                            slotItem.setCount(slotItem.getCount() + 1);
                            carriedItem.setCount(carriedItem.getCount() - 1);
                            if (carriedItem.isEmpty()) {
                                carriedItem = ItemStack.EMPTY;
                            }
                        }
                    }
                } else {
                    // Предметы разные - меняем местами
                    ItemStack temp = slotItem.copy();
                    if (!isRecipeSlot) {
                        playerInventory.setItem(slotIndex, carriedItem.copy());
                    } else {
                        recipeContainer.setItem(slotIndex, carriedItem.copy());
                    }
                    carriedItem = temp;
                    isFromRecipe = isRecipeSlot;
                }
            }
        }
    }
    
    private void returnItem() {
        if (!carriedItem.isEmpty()) {
            if (isFromRecipe) {
                // Ищем пустой слот в рецепте
                for (int i = 0; i < 9; i++) {
                    if (recipeContainer.getItem(i).isEmpty()) {
                        recipeContainer.setItem(i, carriedItem);
                        carriedItem = ItemStack.EMPTY;
                        break;
                    }
                }
            } else {
                // Ищем пустой слот в инвентаре
                for (int i = 0; i < playerInventory.getContainerSize(); i++) {
                    if (playerInventory.getItem(i).isEmpty()) {
                        playerInventory.setItem(i, carriedItem);
                        carriedItem = ItemStack.EMPTY;
                        break;
                    }
                }
            }
            // Если не нашли пустой слот, дропаем предмет
            if (!carriedItem.isEmpty()) {
                carriedItem = ItemStack.EMPTY;
            }
        }
    }
}
