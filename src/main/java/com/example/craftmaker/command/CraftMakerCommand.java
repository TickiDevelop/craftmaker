package com.example.craftmaker.command;

import com.example.craftmaker.reload.CraftMakerReloader;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Регистрация команд /craftmaker и /cm.
 * Обеспечивает доступ к перезагрузке рецептов (только на сервере).
 * Для открытия GUI используйте /cm на клиенте.
 */
public class CraftMakerCommand {
    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * Регистрирует команды при событии RegisterCommandsEvent.
     * Только серверные команды (reload) регистрируются здесь.
     * Клиентские команды (/cm, /craftmaker без аргументов) регистрируются в ClientCommandHandler.
     */
    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        
        // Команда /craftmaker с подкомандами
        dispatcher.register(
            Commands.literal("craftmaker")
                .then(Commands.literal("reload")
                    .executes(CraftMakerCommand::reloadRecipes)
                )
                .then(Commands.literal("save")
                    .then(Commands.argument("filename", StringArgumentType.string())
                        .then(Commands.argument("json", StringArgumentType.greedyString())
                            .executes(CraftMakerCommand::saveRecipe)
                        )
                    )
                )
                .then(Commands.literal("delete")
                    .then(Commands.argument("filename", StringArgumentType.string())
                        .executes(CraftMakerCommand::deleteRecipe)
                    )
                )
                .then(Commands.literal("list")
                    .executes(CraftMakerCommand::listRecipes)
                )
        );
        
        // Команда /cm с подкомандами
        dispatcher.register(
            Commands.literal("cm")
                .then(Commands.literal("reload")
                    .executes(CraftMakerCommand::reloadRecipes)
                )
                .then(Commands.literal("save")
                    .then(Commands.argument("filename", StringArgumentType.string())
                        .then(Commands.argument("json", StringArgumentType.greedyString())
                            .executes(CraftMakerCommand::saveRecipe)
                        )
                    )
                )
                .then(Commands.literal("delete")
                    .then(Commands.argument("filename", StringArgumentType.string())
                        .executes(CraftMakerCommand::deleteRecipe)
                    )
                )
                .then(Commands.literal("list")
                    .executes(CraftMakerCommand::listRecipes)
                )
        );
    }

    /**
     * Обработчик перезагрузки рецептов.
     */
    private static int reloadRecipes(CommandContext<CommandSourceStack> context) {
        try {
            CraftMakerReloader.reload();
            context.getSource().sendSuccess(() -> Component.literal("CraftMaker recipes reloaded successfully!"), true);
            return 1;
        } catch (Exception e) {
            LOGGER.error("Failed to reload CraftMaker recipes", e);
            context.getSource().sendFailure(Component.literal("Failed to reload recipes. Check server logs."));
            return 0;
        }
    }

    /**
     * Обработчик сохранения рецепта с сервера.
     */
    private static int saveRecipe(CommandContext<CommandSourceStack> context) {
        try {
            String filename = StringArgumentType.getString(context, "filename");
            String jsonStr = StringArgumentType.getString(context, "json");
            
            JsonObject json = new Gson().fromJson(jsonStr, JsonObject.class);
            if (json == null) {
                context.getSource().sendFailure(Component.literal("Invalid JSON data"));
                return 0;
            }
            
            com.example.craftmaker.recipe.RecipeFileManager.saveRecipe(filename, json);
            CraftMakerReloader.reload();
            context.getSource().sendSuccess(() -> Component.literal("Recipe saved: " + filename), true);
            return 1;
        } catch (Exception e) {
            LOGGER.error("Failed to save recipe", e);
            context.getSource().sendFailure(Component.literal("Failed to save recipe: " + e.getMessage()));
            return 0;
        }
    }

    /**
     * Обработчик удаления рецепта с сервера.
     */
    private static int deleteRecipe(CommandContext<CommandSourceStack> context) {
        try {
            String filename = StringArgumentType.getString(context, "filename");
            
            com.example.craftmaker.recipe.RecipeFileManager.deleteRecipe(filename);
            CraftMakerReloader.reload();
            context.getSource().sendSuccess(() -> Component.literal("Recipe deleted: " + filename), true);
            return 1;
        } catch (Exception e) {
            LOGGER.error("Failed to delete recipe", e);
            context.getSource().sendFailure(Component.literal("Failed to delete recipe: " + e.getMessage()));
            return 0;
        }
    }

    /**
     * Обработчик списка рецептов с сервера.
     */
    private static int listRecipes(CommandContext<CommandSourceStack> context) {
        try {
            java.util.List<com.example.craftmaker.recipe.RecipeFileManager.RecipeFile> recipes = 
                com.example.craftmaker.recipe.RecipeFileManager.loadAllRecipes();
            
            // Формируем JSON со списком рецептов
            com.google.gson.JsonArray recipesArray = new com.google.gson.JsonArray();
            for (com.example.craftmaker.recipe.RecipeFileManager.RecipeFile rf : recipes) {
                com.google.gson.JsonObject recipeObj = new com.google.gson.JsonObject();
                recipeObj.addProperty("filename", rf.filename);
                recipeObj.add("json", rf.json);
                recipesArray.add(recipeObj);
            }
            
            com.google.gson.JsonObject result = new com.google.gson.JsonObject();
            result.add("recipes", recipesArray);
            String jsonStr = new Gson().toJson(result);
            
            context.getSource().sendSuccess(() -> Component.literal(jsonStr), false);
            return 1;
        } catch (Exception e) {
            LOGGER.error("Failed to list recipes", e);
            context.getSource().sendFailure(Component.literal("Failed to list recipes: " + e.getMessage()));
            return 0;
        }
    }
}
