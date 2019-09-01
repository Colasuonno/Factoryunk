package com.unknown.factoryunk.items.factories.types;

import com.unknown.factoryunk.exceptions.FactoryException;
import com.unknown.factoryunk.items.blueprint.FactoryType;
import com.unknown.factoryunk.items.factories.Factory;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;

import java.util.Set;
import java.util.UUID;

public class LegendaryFactory extends Factory {

    public LegendaryFactory(UUID owner, Location blockLocation, Set<UUID> admins, Material material, int health, FactoryType type, Location center, Location pos1, Location pos2) {
        super(owner, blockLocation, admins, material, health, type, center, pos1, pos2);
    }

    public LegendaryFactory(Material material, FactoryType type, UUID owner, Set<UUID> admins, int health) {
        super(material, type, owner, admins, health);
    }

    @Override
    public void reward(){
        setLastDrop(System.currentTimeMillis());
        reduceHealth(1);
        getLines().modify("Health", ChatColor.GREEN + "Health: " + getHealthPercentage(), ArmorStand.class);
        System.out.println("EHEHEHE pt.2");

    }

}