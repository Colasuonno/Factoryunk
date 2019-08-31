package com.unknown.factoryunk.items.factories;

import com.unknown.factoryunk.items.blueprint.FactoryType;
import com.unknown.factoryunk.utils.LocationUtil;
import com.unknown.factoryunk.utils.YamlConfig;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;

import java.util.HashSet;
import java.util.UUID;

public class Factories {

    private Plugin plugin;

    public Factories(Plugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Loads factories from file
     */
    public void load(boolean reset){

        if (reset)Factory.getFactories().clear();
        FileConfiguration fileConfiguration = YamlConfig.getConfiguration(plugin, "factories");

        for (String section : fileConfiguration.getConfigurationSection("factories").getKeys(false)){

            String path = "factories." + section + ".";

            UUID owner = UUID.fromString(fileConfiguration.getString(path + "owner"));
            Location placed = LocationUtil.convertToLocation(fileConfiguration.getString(path + "placed"));
            Location center = LocationUtil.convertToLocation(fileConfiguration.getString(path + "center"));
            Location pos1 = LocationUtil.convertToLocation(fileConfiguration.getString(path + "pos1"));
            Location pos2 = LocationUtil.convertToLocation(fileConfiguration.getString(path + "pos2"));
            Material material = Material.valueOf(fileConfiguration.getString(path + "material"));
            int health = fileConfiguration.getInt(path + "lastHealth");
            FactoryType type = FactoryType.valueOf(fileConfiguration.getString(path + "type"));

            Factory factory = new Factory(owner, placed, new HashSet<>(), material, health, type, center, pos1, pos2);
            factory.restore(center);
            Factory.getFactories().add(factory);
        }

    }

}
