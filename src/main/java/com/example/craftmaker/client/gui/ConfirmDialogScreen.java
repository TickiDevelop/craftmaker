package com.example.craftmaker.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Простой диалоговый экран с сообщением и кнопкой "Continue".
 */
public class ConfirmDialogScreen extends Screen {
    private final Screen parent;
    private final Component message;

    public ConfirmDialogScreen(Screen parent, String message) {
        super(Component.literal("Message"));
        this.parent = parent;
        this.message = Component.literal(message);
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        this.addRenderableWidget(Button.builder(Component.literal("Continue"), button -> {
            this.minecraft.setScreen(parent);
        }).bounds(centerX - 60, centerY + 20, 120, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        graphics.drawCenteredString(this.font, this.message, centerX, centerY - 20, 0xFFFF5555);
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
