package com.example.craftmaker.client.gui;

import com.example.craftmaker.recipe.RecipeFileManager;
import com.google.gson.JsonObject;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Экран добавления рецепта.
 * Позволяет ввести данные рецепта и сохранить его в crafts/add/.
 */
public class AddRecipeScreen extends Screen {
    private final Screen parent;
    private EditBox[] gridSlots = new EditBox[9];
    private EditBox resultItem;
    private EditBox resultCount;
    private EditBox filename;
    private boolean isShaped = true;

    public AddRecipeScreen(Screen parent) {
        super(Component.translatable("gui.craftmaker.add_recipe.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int startY = 50;
        int slotSize = 30;
        int slotSpacing = 5;

        // Создаём сетку 3x3 для ингредиентов
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int index = row * 3 + col;
                int x = centerX - (3 * slotSize + 2 * slotSpacing) / 2 + col * (slotSize + slotSpacing);
                int y = startY + row * (slotSize + slotSpacing);
                
                gridSlots[index] = new EditBox(this.font, x, y, slotSize, 20, Component.literal("Item ID"));
                gridSlots[index].setMaxLength(64);
                this.addRenderableWidget(gridSlots[index]);
            }
        }

        // Поле для результата (Item ID)
        resultItem = new EditBox(this.font, centerX - 100, startY + 110, 200, 20, Component.literal("Result Item ID"));
        resultItem.setMaxLength(64);
        this.addRenderableWidget(resultItem);

        // Поле для количества результата
        resultCount = new EditBox(this.font, centerX - 100, startY + 140, 200, 20, Component.literal("Count"));
        resultCount.setMaxLength(2);
        resultCount.setValue("1");
        this.addRenderableWidget(resultCount);

        // Поле для имени файла
        filename = new EditBox(this.font, centerX - 100, startY + 170, 200, 20, Component.literal("Filename"));
        filename.setMaxLength(64);
        this.addRenderableWidget(filename);

        // Переключатель Shaped/Shapeless
        this.addRenderableWidget(Button.builder(Component.literal(isShaped ? "Shaped" : "Shapeless"), button -> {
            isShaped = !isShaped;
            button.setMessage(Component.literal(isShaped ? "Shaped" : "Shapeless"));
        }).bounds(centerX - 50, startY + 200, 100, 20).build());

        // Кнопка "Сохранить"
        this.addRenderableWidget(Button.builder(Component.translatable("gui.craftmaker.save"), button -> {
            saveRecipe();
        }).bounds(centerX - 50, startY + 230, 100, 20).build());

        // Кнопка "Назад"
        this.addRenderableWidget(Button.builder(Component.translatable("gui.craftmaker.back"), button -> {
            this.minecraft.setScreen(parent);
        }).bounds(centerX - 50, startY + 260, 100, 20).build());
    }

    /**
     * Сохраняет рецепт в JSON файл и вызывает reload.
     */
    private void saveRecipe() {
        String name = filename.getValue().trim();
        if (name.isEmpty()) {
            name = "recipe_" + System.currentTimeMillis();
        }

        JsonObject json = new JsonObject();
        json.addProperty("type", isShaped ? "minecraft:crafting_shaped" : "minecraft:crafting_shapeless");

        // Для shaped рецептов
        if (isShaped) {
            String[] pattern = new String[3];
            for (int row = 0; row < 3; row++) {
                StringBuilder sb = new StringBuilder();
                for (int col = 0; col < 3; col++) {
                    String itemId = gridSlots[row * 3 + col].getValue().trim();
                    sb.append(itemId.isEmpty() ? " " : (char)('A' + row * 3 + col));
                }
                pattern[row] = sb.toString();
            }
            json.add("pattern", new com.google.gson.JsonArray());
            for (String p : pattern) {
                json.getAsJsonArray("pattern").add(p);
            }

            // Ключи для ингредиентов
            JsonObject key = new JsonObject();
            char currentChar = 'A';
            for (EditBox slot : gridSlots) {
                String itemId = slot.getValue().trim();
                if (!itemId.isEmpty()) {
                    JsonObject itemObj = new JsonObject();
                    itemObj.addProperty("item", itemId);
                    key.add(String.valueOf(currentChar), itemObj);
                }
                currentChar++;
            }
            json.add("key", key);
        } else {
            // Для shapeless рецептов
            json.add("ingredients", new com.google.gson.JsonArray());
            for (EditBox slot : gridSlots) {
                String itemId = slot.getValue().trim();
                if (!itemId.isEmpty()) {
                    JsonObject itemObj = new JsonObject();
                    itemObj.addProperty("item", itemId);
                    json.getAsJsonArray("ingredients").add(itemObj);
                }
            }
        }

        // Результат
        JsonObject result = new JsonObject();
        result.addProperty("id", resultItem.getValue().trim());
        try {
            result.addProperty("count", Integer.parseInt(resultCount.getValue()));
        } catch (NumberFormatException e) {
            result.addProperty("count", 1);
        }
        json.add("result", result);

        // Сохраняем файл
        RecipeFileManager.saveRecipe(name, json);

        // Вызываем reload
        this.minecraft.player.connection.sendCommand("cm reload");

        // Закрываем экран
        this.minecraft.setScreen(parent);
    }

    @Override
    public void render(net.minecraft.client.gui.GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFF);
        
        // Подписи к полям
        graphics.drawString(this.font, Component.translatable("gui.craftmaker.ingredients"), this.width / 2 - 50, 35, 0xFFFFFF);
        graphics.drawString(this.font, Component.translatable("gui.craftmaker.result_item"), this.width / 2 - 100, 100, 0xFFFFFF);
        graphics.drawString(this.font, Component.translatable("gui.craftmaker.result_count"), this.width / 2 - 100, 130, 0xFFFFFF);
        graphics.drawString(this.font, Component.translatable("gui.craftmaker.filename"), this.width / 2 - 100, 160, 0xFFFFFF);
        
        super.render(graphics, mouseX, mouseY, partialTick);
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
