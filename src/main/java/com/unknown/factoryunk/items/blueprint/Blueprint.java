package com.unknown.factoryunk.items.blueprint;

import org.bukkit.inventory.ItemStack;

public interface Blueprint {

    /**
     * Gets the factory reward
     * @return the itemstack reward
     */
    ItemStack getReward();

    /**
     * The rarity of the factory
     * @return rarity
     */
    FactoryType getType();

    /**
     * Generate a new blue print item
     * @return blueprint item
     */
    ItemStack generateBluePrint();

}
