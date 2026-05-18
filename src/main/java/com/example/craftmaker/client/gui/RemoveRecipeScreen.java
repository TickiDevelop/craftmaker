package com.example.craftmaker.client.gui;

import com.example.craftmaker.recipe.RecipeFileManager;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Экран удаления рецепта.
 * Позволяет ввести Recipe ID и создать файл удаления в crafts/remove/.
 */
public class RemoveRecipeScreen extends Screen {
    private final Screen parent;
    private EditBox recipeIdField;

    public RemoveRecipeScreen(Screen parent) {
        super(Component.translatable("gui.craftmaker.remove_recipe.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // Поле для ввода Recipe ID
        recipeIdField = new EditBox(this.font, centerX - 100, centerY - 30, 200, 20, Component.literal("Recipe ID"));
        recipeIdField.setMaxLength(256);
        recipeIdField.setResponder(value -> {});
        this.addRenderableWidget(recipeIdField);

        // Кнопка "Удалить"
        this.addRenderableWidget(Button.builder(Component.translatable("gui.craftmaker.delete"), button -> {
            deleteRecipe();
        }).bounds(centerX - 50, centerY + 10, 100, 20).build());

        // Кнопка "Назад"
        this.addRenderableWidget(Button.builder(Component.translatable("gui.craftmaker.back"), button -> {
            this.minecraft.setScreen(parent);
        }).bounds(centerX - 50, centerY + 40, 100, 20).build());
    }

    /**
     * Создаёт файл удаления рецепта и вызывает reload.
     */
    private void deleteRecipe() {
        String recipeId = recipeIdField.getValue().trim();
        
        if (recipeId.isEmpty()) {
            return;
        }

        // Удаляем файл рецепта
        RecipeFileManager.deleteRecipe(recipeId);

        // Вызываем reload
        this.minecraft.player.connection.sendCommand("cm reload");

        // Закрываем экран
        this.minecraft.setScreen(parent);
    }

    @Override
    public void render(net.minecraft.client.gui.GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFF);
        graphics.drawString(this.font, Component.translatable("gui.craftmaker.recipe_id"), this.width / 2 - 100, this.height / 2 - 50, 0xFFFFFF);
        
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
