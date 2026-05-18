package com.example.craftmaker.client.gui;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Главный экран CraftMaker GUI.
 * Отображает кнопки навигации: Добавить, Список, Reload, Закрыть.
 */
public class CraftMakerScreen extends Screen {
    
    public CraftMakerScreen() {
        super(Component.translatable("gui.craftmaker.title"));
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int buttonWidth = 200;
        int buttonHeight = 20;
        int spacing = 25;

        // Кнопка "Добавить рецепт"
        this.addRenderableWidget(Button.builder(Component.translatable("gui.craftmaker.add_recipe"), button -> {
            this.minecraft.setScreen(new AddRecipeCustomScreen(this.minecraft.player.getInventory(), this));
        }).bounds(centerX - buttonWidth / 2, centerY - spacing * 2, buttonWidth, buttonHeight).build());

        // Кнопка "Список рецептов"
        this.addRenderableWidget(Button.builder(Component.translatable("gui.craftmaker.recipe_list"), button -> {
            this.minecraft.setScreen(new RecipeListScreen(this));
        }).bounds(centerX - buttonWidth / 2, centerY - spacing, buttonWidth, buttonHeight).build());

        // Кнопка "Reload"
        this.addRenderableWidget(Button.builder(Component.translatable("gui.craftmaker.reload"), button -> {
            this.minecraft.player.connection.sendCommand("cm reload");
        }).bounds(centerX - buttonWidth / 2, centerY, buttonWidth, buttonHeight).build());

        // Кнопка "Закрыть"
        this.addRenderableWidget(Button.builder(Component.translatable("gui.craftmaker.close"), button -> {
            this.onClose();
        }).bounds(centerX - buttonWidth / 2, centerY + spacing, buttonWidth, buttonHeight).build());
    }

    @Override
    public void render(net.minecraft.client.gui.GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFF);
        super.render(graphics, mouseX, mouseY, partialTick);
    }
    
    @Override
    public void removed() {
        super.removed();
    }
    
    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
