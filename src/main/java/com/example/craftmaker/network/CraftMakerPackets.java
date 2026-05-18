package com.example.craftmaker.network;

import com.example.craftmaker.recipe.RecipeFileManager;
import com.example.craftmaker.reload.CraftMakerReloader;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.ArrayList;
import java.util.List;

/**
 * Сетевая система для синхронизации рецептов между клиентом и сервером.
 * Использует новый API NeoForge на основе CustomPacketPayload и StreamCodec.
 */
public class CraftMakerPackets {
    public static final Identifier SAVE_RECIPE_ID = Identifier.fromNamespaceAndPath("craftmaker", "save_recipe");
    public static final Identifier DELETE_RECIPE_ID = Identifier.fromNamespaceAndPath("craftmaker", "delete_recipe");
    public static final Identifier REQUEST_RECIPES_ID = Identifier.fromNamespaceAndPath("craftmaker", "request_recipes");
    public static final Identifier RECIPES_LIST_ID = Identifier.fromNamespaceAndPath("craftmaker", "recipes_list");

    /**
     * Пакет для сохранения рецепта на сервере.
     */
    public record SaveRecipePacket(String filename, String json) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<SaveRecipePacket> TYPE = new CustomPacketPayload.Type<>(SAVE_RECIPE_ID);
        
        public static final net.minecraft.network.codec.StreamCodec<RegistryFriendlyByteBuf, SaveRecipePacket> STREAM_CODEC = 
            net.minecraft.network.codec.StreamCodec.composite(
                net.minecraft.network.codec.ByteBufCodecs.STRING_UTF8,
                SaveRecipePacket::filename,
                net.minecraft.network.codec.ByteBufCodecs.STRING_UTF8,
                SaveRecipePacket::json,
                SaveRecipePacket::new
            );

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /**
     * Пакет для удаления рецепта с сервера.
     */
    public record DeleteRecipePacket(String filename) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<DeleteRecipePacket> TYPE = new CustomPacketPayload.Type<>(DELETE_RECIPE_ID);
        
        public static final net.minecraft.network.codec.StreamCodec<RegistryFriendlyByteBuf, DeleteRecipePacket> STREAM_CODEC = 
            net.minecraft.network.codec.StreamCodec.composite(
                net.minecraft.network.codec.ByteBufCodecs.STRING_UTF8,
                DeleteRecipePacket::filename,
                DeleteRecipePacket::new
            );

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /**
     * Пакет для запроса списка рецептов с сервера.
     */
    public record RequestRecipesPacket() implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<RequestRecipesPacket> TYPE = new CustomPacketPayload.Type<>(REQUEST_RECIPES_ID);
        
        public static final net.minecraft.network.codec.StreamCodec<RegistryFriendlyByteBuf, RequestRecipesPacket> STREAM_CODEC = 
            net.minecraft.network.codec.StreamCodec.unit(new RequestRecipesPacket());

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /**
     * Пакет для отправки списка рецептов с сервера клиенту.
     */
    public record RecipesListPacket(List<String> filenames, List<String> jsons) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<RecipesListPacket> TYPE = new CustomPacketPayload.Type<>(RECIPES_LIST_ID);
        
        public static final net.minecraft.network.codec.StreamCodec<RegistryFriendlyByteBuf, RecipesListPacket> STREAM_CODEC = 
            net.minecraft.network.codec.StreamCodec.composite(
                net.minecraft.network.codec.ByteBufCodecs.collection(ArrayList::new, net.minecraft.network.codec.ByteBufCodecs.STRING_UTF8),
                RecipesListPacket::filenames,
                net.minecraft.network.codec.ByteBufCodecs.collection(ArrayList::new, net.minecraft.network.codec.ByteBufCodecs.STRING_UTF8),
                RecipesListPacket::jsons,
                RecipesListPacket::new
            );

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /**
     * Регистрация пакетов.
     */
    public static void register(PayloadRegistrar registrar) {
        // Пакеты от клиента к серверу
        registrar.playToServer(
            SaveRecipePacket.TYPE,
            SaveRecipePacket.STREAM_CODEC,
            CraftMakerPackets::handleSaveRecipe
        );

        registrar.playToServer(
            DeleteRecipePacket.TYPE,
            DeleteRecipePacket.STREAM_CODEC,
            CraftMakerPackets::handleDeleteRecipe
        );

        registrar.playToServer(
            RequestRecipesPacket.TYPE,
            RequestRecipesPacket.STREAM_CODEC,
            CraftMakerPackets::handleRequestRecipes
        );

        // Пакеты от сервера к клиенту
        registrar.playToClient(
            RecipesListPacket.TYPE,
            RecipesListPacket.STREAM_CODEC,
            CraftMakerPackets::handleRecipesList
        );
    }



    private static void handleSaveRecipe(SaveRecipePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.flow().isServerbound()) {
                JsonObject json = new Gson().fromJson(packet.json, JsonObject.class);
                if (json != null) {
                    RecipeFileManager.saveRecipe(packet.filename, json);
                    CraftMakerReloader.reload();
                    if (context.player() != null) {
                        context.player().displayClientMessage(Component.literal("Recipe saved: " + packet.filename), false);
                    }
                }
            }
        });
    }

    private static void handleDeleteRecipe(DeleteRecipePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.flow().isServerbound()) {
                RecipeFileManager.deleteRecipe(packet.filename);
                CraftMakerReloader.reload();
                if (context.player() != null) {
                    context.player().displayClientMessage(Component.literal("Recipe deleted: " + packet.filename), false);
                }
            }
        });
    }

    private static void handleRequestRecipes(RequestRecipesPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.flow().isServerbound()) {
                List<RecipeFileManager.RecipeFile> recipes = RecipeFileManager.loadAllRecipes();
                List<String> filenames = new ArrayList<>();
                List<String> jsons = new ArrayList<>();
                for (RecipeFileManager.RecipeFile rf : recipes) {
                    filenames.add(rf.filename);
                    jsons.add(new Gson().toJson(rf.json));
                }
                // Отправляем ответ клиенту через PacketDistributor
                if (context.player() instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                    PacketDistributor.sendToPlayer(serverPlayer, new RecipesListPacket(filenames, jsons));
                }
            }
        });
    }

    private static void handleRecipesList(RecipesListPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.flow().isClientbound()) {
                // Преобразуем полученные данные в список RecipeFile
                List<RecipeFileManager.RecipeFile> recipes = new ArrayList<>();
                for (int i = 0; i < packet.filenames.size(); i++) {
                    JsonObject json = new Gson().fromJson(packet.jsons.get(i), JsonObject.class);
                    if (json != null) {
                        recipes.add(new RecipeFileManager.RecipeFile(packet.filenames.get(i), json));
                    }
                }
                // Передаем рецепты напрямую в GUI без сохранения локально
                net.minecraft.client.Minecraft.getInstance().execute(() -> {
                    if (net.minecraft.client.Minecraft.getInstance().screen instanceof com.example.craftmaker.client.gui.RecipeListScreen screen) {
                        screen.setServerRecipes(recipes);
                    }
                });
            }
        });
    }

    /**
     * Отправить запрос на список рецептов с сервера (клиентский метод).
     */
    public static void sendRequestRecipesToServer() {
        var mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.getConnection() != null) {
            mc.getConnection().send(new RequestRecipesPacket());
        }
    }

    /**
     * Отправить рецепт на сервер для сохранения (клиентский метод).
     */
    public static void sendSaveRecipeToServer(String filename, String json) {
        var mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.getConnection() != null) {
            mc.getConnection().send(new SaveRecipePacket(filename, json));
        }
    }

    /**
     * Отправить запрос на удаление рецепта с сервера (клиентский метод).
     */
    public static void sendDeleteRecipeToServer(String filename) {
        var mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.getConnection() != null) {
            mc.getConnection().send(new DeleteRecipePacket(filename));
        }
    }
}

