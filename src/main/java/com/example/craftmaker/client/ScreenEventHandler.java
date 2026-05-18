package com.example.craftmaker.client;

import com.example.craftmaker.client.gui.AddRecipeCustomScreen;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Event handler для гарантированного освобождения мыши при открытии GUI
 * и для обработки событий мыши в AddRecipeCustomScreen.
 */
@EventBusSubscriber(Dist.CLIENT)
public class ScreenEventHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ScreenEventHandler.class);

    @SubscribeEvent
    public static void onScreenOpen(ScreenEvent.Opening event) {
        if (event.getNewScreen() != null) {
            LOGGER.info("Screen opening: {}, releasing mouse", event.getNewScreen().getClass().getSimpleName());
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.mouseHandler != null) {
                minecraft.mouseHandler.releaseMouse();
            }
        }
    }

    @SubscribeEvent
    public static void onMouseButtonPressedPre(ScreenEvent.MouseButtonPressed.Pre event) {
        if (event.getScreen() instanceof AddRecipeCustomScreen screen) {
            int mx = (int) event.getMouseX();
            int my = (int) event.getMouseY();
            int button = event.getButton();
            
            if (screen.handleMouseClick(mx, my, button)) {
                event.setCanceled(true);
            }
        }
    }
}


