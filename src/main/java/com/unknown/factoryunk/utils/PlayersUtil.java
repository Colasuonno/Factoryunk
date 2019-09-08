package com.unknown.factoryunk.utils;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class PlayersUtil {

    /**
     * Checks if the player inventory is full
     * @param player the inventory holder
     * @return if the inventory is full
     */
    public static boolean hasFullInventory(Player player){
        return player.getInventory().firstEmpty() == -1;
    }

    /**
     * Chekcs if a player can hold an itemstack
     * @param player the inventory holder
     * @param material the material of the item
     * @param amount the amount the the item
     * @return if the player can hold an item
     */
    public static boolean hasFreeSpaceForItemStack(Player player, Material material, int amount){
        if (!hasFullInventory(player)) return true;
        for (ItemStack itemStack : player.getInventory().getContents()){
            if (itemStack != null && itemStack.getType().equals(material) && itemStack.getAmount() <= 64-amount) return true;
        }
        return false;
    }

    /**
     * Chekcs if a player can hold an itemstack
     * @see #hasFreeSpaceForItemStack(Player, Material, int)
     */
    public static boolean canHoldItem(Player player, ItemStack itemStack){
        return hasFreeSpaceForItemStack(player, itemStack.getType(), itemStack.getAmount());
    }

}
