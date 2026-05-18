package com.example.craftmaker.client.gui;

import com.example.craftmaker.inventory.RecipeMenu;
import com.example.craftmaker.recipe.RecipeFileManager;
import com.example.craftmaker.reload.CraftMakerReloader;
import com.google.gson.JsonObject;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * Экран создания рецептов с контейнером.
 * Показывает слоты для ингредиентов 3x3, результат и инвентарь игрока.
 */
public class AddRecipeContainerScreen extends AbstractContainerScreen<RecipeMenu> {
    
    public AddRecipeContainerScreen(RecipeMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 200;
    }
    
    @Override
    protected void init() {
        this.imageWidth = 200;
        // Кнопка "Сохранить рецепт"
        this.addRenderableWidget(Button.builder(Component.literal("Save"), button -> {
            saveRecipe();
        }).bounds(this.leftPos + 8, this.topPos + 170, 160, 20).build());
    }
    
    /**
     * Сохраняет рецепт из слотов в JSON файл.
     */
    private void saveRecipe() {
        RecipeMenu menu = this.menu;
        JsonObject recipeJson = new JsonObject();
        
        // Тип рецепта
        recipeJson.addProperty("type", "minecraft:crafting_shaped");
        
        // Pattern - определяем форму рецепта
        String[] pattern = new String[3];
        for (int row = 0; row < 3; row++) {
            StringBuilder rowStr = new StringBuilder();
            for (int col = 0; col < 3; col++) {
                ItemStack stack = menu.getSlot(row * 3 + col).getItem();
                if (!stack.isEmpty()) {
                    rowStr.append((char) ('A' + col));
                } else {
                    rowStr.append(' ');
                }
            }
            pattern[row] = rowStr.toString();
        }
        recipeJson.add("pattern", new com.google.gson.JsonArray());
        recipeJson.getAsJsonArray("pattern").add(pattern[0]);
        recipeJson.getAsJsonArray("pattern").add(pattern[1]);
        recipeJson.getAsJsonArray("pattern").add(pattern[2]);
        
        // Key - определяем ключи для ингредиентов
        JsonObject key = new JsonObject();
        char currentChar = 'A';
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                ItemStack stack = menu.getSlot(row * 3 + col).getItem();
                if (!stack.isEmpty()) {
                    String itemId = stack.getItem().toString();
                    JsonObject itemObj = new JsonObject();
                    itemObj.addProperty("item", itemId);
                    key.add(String.valueOf(currentChar), itemObj);
                    currentChar++;
                }
            }
        }
        recipeJson.add("key", key);
        
        // Result
        ItemStack resultStack = menu.getSlot(9).getItem(); // Слот результата
        if (!resultStack.isEmpty()) {
            JsonObject result = new JsonObject();
            result.addProperty("id", resultStack.getItem().toString());
            result.addProperty("count", resultStack.getCount());
            recipeJson.add("result", result);
        } else {
            this.minecraft.player.displayClientMessage(Component.literal("Error: result slot is empty!"), false);
            return;
        }
        
        // Сохраняем в файл
        String filename = "recipe_" + System.currentTimeMillis() + ".json";
        try {
            RecipeFileManager.saveRecipe(filename, recipeJson);
            this.minecraft.player.displayClientMessage(Component.literal("Recipe saved: " + filename), false);
            CraftMakerReloader.reload();
            // Закрываем экран после успешного сохранения
            this.onClose();
        } catch (Exception e) {
            this.minecraft.player.displayClientMessage(Component.literal("Save error: " + e.getMessage()), false);
            e.printStackTrace();
        }
    }
    
    @Override
    public void onClose() {
        super.onClose();
    }
    
    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        // Рисуем фон слотов вручную
        int x = this.leftPos;
        int y = this.topPos;
        
        // Фон слотов ингредиентов 3x3
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                graphics.fill(x + 8 + col * 18, y + 18 + row * 18, x + 26 + col * 18, y + 36 + row * 18, 0xFF8B8B8B);
                graphics.fill(x + 9 + col * 18, y + 19 + row * 18, x + 25 + col * 18, y + 35 + row * 18, 0xFF3C3C3C);
            }
        }
        
        // Фон слота результата
        graphics.fill(x + 124, y + 35, x + 142, y + 53, 0xFF8B8B8B);
        graphics.fill(x + 125, y + 36, x + 141, y + 52, 0xFF3C3C3C);
        
        // Фон инвентаря игрока
        graphics.fill(x + 8, y + 84, x + 170, y + 142, 0xFF8B8B8B);
        graphics.fill(x + 9, y + 85, x + 169, y + 141, 0xFFC6C6C6);
        
        // Слоты основного инвентаря 27 слотов
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                graphics.fill(x + 9 + col * 18, y + 85 + row * 18, x + 25 + col * 18, y + 101 + row * 18, 0xFF8B8B8B);
                graphics.fill(x + 10 + col * 18, y + 86 + row * 18, x + 24 + col * 18, y + 100 + row * 18, 0xFF3C3C3C);
            }
        }
        
        // Слоты hotbar 9 слотов
        for (int col = 0; col < 9; col++) {
            graphics.fill(x + 9 + col * 18, y + 143, x + 25 + col * 18, y + 159, 0xFF8B8B8B);
            graphics.fill(x + 10 + col * 18, y + 144, x + 24 + col * 18, y + 158, 0xFF3C3C3C);
        }
    }
    
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
    }
    
    @Override
    public void removed() {
        super.removed();
    }
    
    @Override
    public boolean isPauseScreen() {
        return true;
    }
}
