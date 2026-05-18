package com.example.craftmaker.client;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Клиентские команды для CraftMaker.
 * Регистрирует команду /cm для открытия GUI на клиенте.
 */
@EventBusSubscriber(Dist.CLIENT)
public class ClientCommandHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientCommandHandler.class);

    @SubscribeEvent
    public static void onRegisterCommands(RegisterClientCommandsEvent event) {
        LOGGER.info("Registering client commands...");
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        // Команда /cm
        dispatcher.register(
            Commands.literal("cm")
                .executes(ClientCommandHandler::openGui)
        );

        // Команда /craftmaker
        dispatcher.register(
            Commands.literal("craftmaker")
                .executes(ClientCommandHandler::openGui)
        );
    }

    /**
     * Открывает GUI CraftMaker.
     */
    private static int openGui(CommandContext<CommandSourceStack> context) {
        LOGGER.info("Executing /cm command, opening GUI...");
        try {
            Minecraft minecraft = Minecraft.getInstance();
            // Открываем экран на следующем тике чтобы избежать конфликта с ChatScreen
            minecraft.execute(() -> {
                LOGGER.info("Setting CraftMakerScreen...");
                minecraft.setScreen(new com.example.craftmaker.client.gui.CraftMakerScreen());
            });
            return 1;
        } catch (Exception e) {
            LOGGER.error("Failed to open GUI", e);
            e.printStackTrace();
        }
        return 0;
    }
}
