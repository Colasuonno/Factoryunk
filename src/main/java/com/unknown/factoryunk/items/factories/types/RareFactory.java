package com.unknown.factoryunk.items.factories.types;

import com.unknown.factoryunk.items.blueprint.FactoryType;
import com.unknown.factoryunk.items.factories.Factory;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.plugin.Plugin;

import java.util.Set;
import java.util.UUID;

public class RareFactory  extends Factory {

    public RareFactory(Plugin plugin, String lastSchem, int itemsCollected, long created, UUID owner, Location blockLocation, Set<UUID> admins, Material material, int health, FactoryType type, Location center, Location pos1, Location pos2) {
        super(plugin, lastSchem, itemsCollected, created, owner, blockLocation, admins, material, health, type, center, pos1, pos2);
        setDelayInMillis(4000L);
    }

    public RareFactory(Material material, FactoryType type, UUID owner, Set<UUID> admins, int health) {
        super(material, type, owner, admins, health);
    }

    @Override
    public void reward() {
        setLastDrop(System.currentTimeMillis());
        reduceHealth(1);
        collect(1);
        getLines().modify("Health", ChatColor.GREEN + "Health: " + getHealthPercentage(), ArmorStand.class);
    }

}