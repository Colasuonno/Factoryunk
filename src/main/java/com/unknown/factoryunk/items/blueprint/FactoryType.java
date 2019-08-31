package com.unknown.factoryunk.items.blueprint;

import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.List;

/**
 * Default values
 */
public enum FactoryType {


    COMMON(Material.COAL, Material.IRON_INGOT),
    RARE(Material.GOLD_INGOT, Material.REDSTONE),
    LEGENDARY(Material.EMERALD, Material.DIAMOND);

    @Getter private List<Material> materials;

    FactoryType(Material... materials) {
        this.materials = Arrays.asList(materials);
    }

    /**
     * Checks if the material is valid for the enum given
     * @param material type
     * @return true if it is a valid item
     */
    public boolean isValid(Material material){
        return materials.contains(material);
    }


    /**
     * Checks if the itemstack given is a valid MATERIAL
     * @param material the material
     * @return if it is a valid material
     */
    public static boolean isMaterialValid(Material material){
        for (FactoryType type : values()){
            if (type.isValid(material)) return true;
        }
        return false;
    }
    /**
     * Checks if the itemstack given is a valid BLUEPRINT
     * @param itemStack the blueprint
     * @return if it is a valid blueprint
     */
    public static boolean isValid(ItemStack itemStack){
        if (itemStack == null || !itemStack.getType().equals(Material.PAPER) || !itemStack.hasItemMeta() || !itemStack.getItemMeta().hasDisplayName()) return false;
        return itemStack.getItemMeta().getDisplayName().contains("Blueprint");
    }


    /**
     * Gets the FactoryType from material
     * @param material of the factory
     * @return the factory type, can be null
     */
    public static FactoryType fromMaterial(Material material){
        for (FactoryType type : values()){
            if (type.isValid(material)) return type;
        }
        return null;
    }

}
