package com.example.craftmaker.inventory;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Меню для создания рецептов.
 * Содержит слоты для ингредиентов (3x3) и результата + инвентарь игрока.
 */
public class RecipeMenu extends AbstractContainerMenu {
    
    private static final int INGREDIENT_SLOTS_START = 0;
    private static final int INGREDIENT_SLOTS_COUNT = 9;
    private static final int RESULT_SLOT = 9;
    private static final int PLAYER_INVENTORY_START = 10;
    private static final int PLAYER_INVENTORY_END = 46;
    
    private final RecipeContainer recipeContainer;
    
    public RecipeMenu(int containerId, Inventory playerInventory) {
        super(MenuType.CRAFTING, containerId);
        
        this.recipeContainer = new RecipeContainer();
        
        // Добавляем слоты ингредиентов 3x3
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                this.addSlot(new RecipeSlot(recipeContainer, row * 3 + col, 8 + col * 18, 18 + row * 18));
            }
        }
        
        // Добавляем слот результата
        this.addSlot(new RecipeSlot(recipeContainer, RESULT_SLOT, 124, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false; // Нельзя класть в слот результата
            }
        });
        
        // Добавляем инвентарь игрока
        this.addPlayerInventorySlots(playerInventory, 84);
    }
    
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();
            if (index < PLAYER_INVENTORY_START) {
                // Из контейнера в инвентарь
                if (!this.moveItemStackTo(itemstack1, PLAYER_INVENTORY_START, PLAYER_INVENTORY_END, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Из инвентаря в контейнер
                if (!this.moveItemStackTo(itemstack1, INGREDIENT_SLOTS_START, RESULT_SLOT, false)) {
                    return ItemStack.EMPTY;
                }
            }
            if (itemstack1.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return itemstack;
    }
    
    @Override
    public boolean stillValid(Player player) {
        return true;
    }
    
    /**
     * Добавляет инвентарь игрока (36 слотов).
     */
    protected void addPlayerInventorySlots(Inventory playerInventory, int yOffset) {
        int startX = 8;
        int startY = yOffset;
        
        // Основной инвентарь (27 слотов)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, startX + col * 18, startY + row * 18));
            }
        }
        
        // Hotbar (9 слотов)
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, startX + col * 18, startY + 58));
        }
    }
    
    /**
     * Специальный слот для рецептов - позволяет класть любые предметы.
     */
    public static class RecipeSlot extends Slot {
        public RecipeSlot(net.minecraft.world.Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }
        
        @Override
        public boolean mayPlace(ItemStack stack) {
            return true; // Разрешаем класть любые предметы
        }
        
        @Override
        public boolean mayPickup(Player player) {
            return true; // Разрешаем забирать предметы
        }
        
        @Override
        public int getMaxStackSize(ItemStack stack) {
            return 64; // Максимальный размер стака
        }
    }
}
