package com.example.craftmaker.inventory;

import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Контейнер для хранения предметов в слотах рецепта.
 */
public class RecipeContainer implements Container {
    
    private final NonNullList<ItemStack> items = NonNullList.withSize(10, ItemStack.EMPTY); // 9 ингредиентов + результат
    
    @Override
    public int getContainerSize() {
        return items.size();
    }
    
    @Override
    public boolean isEmpty() {
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }
    
    @Override
    public ItemStack getItem(int slot) {
        return slot >= 0 && slot < items.size() ? items.get(slot) : ItemStack.EMPTY;
    }
    
    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack stack = getItem(slot);
        if (!stack.isEmpty()) {
            if (stack.getCount() <= amount) {
                setItem(slot, ItemStack.EMPTY);
            } else {
                stack = stack.split(amount);
                setChanged();
            }
        }
        return stack;
    }
    
    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack stack = getItem(slot);
        setItem(slot, ItemStack.EMPTY);
        return stack;
    }
    
    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot >= 0 && slot < items.size()) {
            items.set(slot, stack);
            if (stack.getCount() > getMaxStackSize()) {
                stack.setCount(getMaxStackSize());
            }
            setChanged();
        }
    }
    
    @Override
    public int getMaxStackSize() {
        return 64;
    }
    
    @Override
    public void setChanged() {
        // Контейнер изменился
    }
    
    @Override
    public boolean stillValid(Player player) {
        return true;
    }
    
    @Override
    public void clearContent() {
        items.clear();
    }
}
