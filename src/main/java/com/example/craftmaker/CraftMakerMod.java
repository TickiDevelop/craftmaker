package com.example.craftmaker;

import com.example.craftmaker.command.CraftMakerCommand;
import com.example.craftmaker.config.CraftMakerConfig;
import com.example.craftmaker.network.CraftMakerPackets;
import com.example.craftmaker.recipe.RecipeFileManager;
import com.example.craftmaker.reload.CraftMakerReloader;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/**
 * Главный класс мода CraftMaker.
 * Точка входа для мода, регистрация событий и команд.
 */
@Mod("craftmaker")
public class CraftMakerMod {

    public CraftMakerMod(ModContainer container) {
        // Регистрация конфигурации
        container.registerConfig(ModConfig.Type.COMMON, CraftMakerConfig.SPEC);
        
        // Подписка на события NeoForge шины
        NeoForge.EVENT_BUS.addListener(this::onServerStarting);
        NeoForge.EVENT_BUS.addListener(this::onPlayerLoggedIn);
        NeoForge.EVENT_BUS.addListener(CraftMakerCommand::register);
    }

    /**
     * Обработчик события старта сервера.
     * Создаёт папку crafts/ и загружает рецепты.
     */
    @SubscribeEvent
    private void onServerStarting(ServerStartingEvent event) {
        RecipeFileManager.ensureFolders();
        CraftMakerReloader.reload();
    }

    /**
     * Обработчик события входа игрока в мир.
     * Отправляет приветственное сообщение с версией мода.
     */
    @SubscribeEvent
    private void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (CraftMakerConfig.showWelcomeMessage.get()) {
            event.getEntity().displayClientMessage(
                Component.literal("[CraftMaker] Version 1.0.0 installed! Use /craftmaker to open recipe editor."),
                false
            );
        }
    }

    /**
     * Регистрация сетевых пакетов.
     */
    @EventBusSubscriber(modid = "craftmaker")
    public static class ModEvents {
        @SubscribeEvent
        public static void onRegisterPayloadHandlers(RegisterPayloadHandlersEvent event) {
            CraftMakerPackets.register(event.registrar("1"));
        }

        @SubscribeEvent
        public static void onModConfigEvent(ModConfigEvent event) {
            if (event.getConfig().getModId().equals("craftmaker")) {
                // Конфиг был перезагружен
            }
        }
    }
}
