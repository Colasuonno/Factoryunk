package com.unknown.factoryunk.items.factories;

import com.unknown.factoryunk.exceptions.FactoryException;
import com.unknown.factoryunk.items.blueprint.FactoryType;
import com.unknown.factoryunk.utils.StringUtils;
import lombok.Data;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

@Data public class FactoryItem {

    private Material material;
    private FactoryType type;

    /**
     * Deserialize an item to a blueprint
     * @throws FactoryException thrown if a blueprint is not valid
     * @param itemStack to be parsed
     */
    public FactoryItem(ItemStack itemStack) {
        if (FactoryType.isValid(itemStack)){
            String name = itemStack.getItemMeta().getDisplayName();
            this.material = Material.valueOf(StringUtils.getStringBetweenTwoStrings(ChatColor.stripColor(name), "(", ")").toUpperCase());
            this.type = FactoryType.fromMaterial(this.material);
        } else throw new FactoryException(itemStack + " is an invalid Blueprint");
    }

    /**
     * Deserialize an material to a FactoryItem
     * @throws FactoryException thrown if a blueprint is not valid
     * @param material to be parsed
     */
    public FactoryItem(Material material) {
        if (FactoryType.isMaterialValid(material)){
            this.material = material;
            this.type = FactoryType.fromMaterial(this.material);
        } else throw new FactoryException(material + " is an invalid Material");
    }

}
