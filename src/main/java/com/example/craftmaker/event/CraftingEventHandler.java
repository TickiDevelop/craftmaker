package com.example.craftmaker.event;

import net.neoforged.fml.common.EventBusSubscriber;

/**
 * Обработчик событий крафта.
 * Кастомные рецепты теперь инжектируются напрямую в RecipeManager через CraftMakerReloader —
 * перехватывать ItemCraftedEvent больше не нужно.
 */
@EventBusSubscriber(modid = "craftmaker")
public class CraftingEventHandler {
    // Логика перенесена в CraftMakerReloader (рефлексионная инъекция в RecipeManager).
}
