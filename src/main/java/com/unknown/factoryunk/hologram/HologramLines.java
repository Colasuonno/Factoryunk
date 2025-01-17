package com.unknown.factoryunk.hologram;

import com.unknown.factoryunk.utils.StringUtils;
import lombok.Data;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;

@Data public class HologramLines {

    private String[] lines;
    private List<Entity> entities = new ArrayList<>();

    public HologramLines(String... lines) {
        this.lines = lines;
    }

    /**
     * Display the hologram with an offset
     * @param center the location
     * @param yOffset the amount of negative blocks between an hologram an another
     */
    public List<Entity> show(Plugin plugin, Location center, double yOffset){

        for (int i = 0; i < lines.length; i++){
            String text = lines[i];
            ArmorStand armorStand = (ArmorStand) center.getWorld().spawnEntity(center.clone().subtract(0.0D, text.contains("<") ? i*yOffset - (yOffset + (yOffset/2)) : i*yOffset, 0.0D), EntityType.ARMOR_STAND);
            armorStand.setMetadata("destroy$load", new FixedMetadataValue(plugin, "yes!"));
            armorStand.setVisible(false);
            armorStand.setGravity(false);
            if (text.contains("<")){
                Material material = Material.valueOf(StringUtils.getStringBetweenTwoStrings(text, "<", ">"));
                Item item = center.getWorld().dropItemNaturally(center, new ItemStack(material));
                item.setPickupDelay(Integer.MAX_VALUE);
                armorStand.setPassenger(item);
                armorStand.setCustomNameVisible(false);
                entities.add(item);
            } else {
                armorStand.setCustomNameVisible(true);
                armorStand.setCustomName(ChatColor.translateAlternateColorCodes('&', text));
            }
            entities.add(armorStand);
        }

        return entities;
    }

    /**
     * Modify an hologram
     * @param contains the string contained in the string
     * @param replaceValue new text value
     * @param classz class of the entity that we want to modify
     */
    public void modify(String contains, String replaceValue, Class<? extends Entity> classz){
        for (Entity entity : entities){
            if (classz.isAssignableFrom(entity.getClass())){
               if (entity.getCustomName() != null && entity.getCustomName().contains(contains)) entity.setCustomName(replaceValue);
            }
        }
    }

    /**
     * Destroy all the holograms
     */
    public void destroy(){

        for (Entity entity : entities){
            if (entity.getPassenger() != null){
                Entity passenger = entity.getPassenger();
                entity.getPassenger().eject();
                passenger.remove();
            }
            entity.remove();
        }

    }

}
