package com.unknown.factoryunk.items.factories;

import com.sun.org.apache.regexp.internal.RE;
import com.unknown.factoryunk.items.blueprint.FactoryType;
import com.unknown.factoryunk.items.factories.types.CommonFactory;
import com.unknown.factoryunk.items.factories.types.LegendaryFactory;
import com.unknown.factoryunk.utils.LocationUtil;
import com.unknown.factoryunk.utils.YamlConfig;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashSet;
import java.util.UUID;

public class Factories {

    private Plugin plugin;

    public Factories(Plugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Loads factories from file
     * We NEED to delay the load due to any world loading errors
     */
    public void load(boolean reset) {

        new BukkitRunnable(){
            @Override
            public void run() {
                if (reset) Factory.getFactories().clear();
                FileConfiguration fileConfiguration = YamlConfig.getConfiguration(plugin, "factories");

                for (String section : fileConfiguration.getConfigurationSection("factories").getKeys(false)) {

                    String path = "factories." + section + ".";

                    long created = Long.valueOf(section);
                    UUID owner = UUID.fromString(fileConfiguration.getString(path + "owner"));
                    Location placed = LocationUtil.convertToLocation(fileConfiguration.getString(path + "placed"));
                    Location center = LocationUtil.convertToLocation(fileConfiguration.getString(path + "center"));
                    Location pos1 = LocationUtil.convertToLocation(fileConfiguration.getString(path + "pos1"));
                    Location pos2 = LocationUtil.convertToLocation(fileConfiguration.getString(path + "pos2"));
                    Material material = Material.valueOf(fileConfiguration.getString(path + "material"));
                    int health = fileConfiguration.getInt(path + "lastHealth");
                    FactoryType type = FactoryType.valueOf(fileConfiguration.getString(path + "type"));
                    int collectedItems = fileConfiguration.getInt(path +"collectedItems");

                    Factory factory;

                    switch (type) {
                        case LEGENDARY:
                            factory = new LegendaryFactory(plugin,collectedItems, created, owner, placed, new HashSet<>(), material, health, type, center, pos1, pos2);
                            break;
                        case COMMON:
                            factory = new CommonFactory(plugin, collectedItems,  created, owner, placed, new HashSet<>(), material, health, type, center, pos1, pos2);
                            break;
                        default:
                            factory = new Factory(plugin, collectedItems, created, owner, placed, new HashSet<>(), material, health, type, center, pos1, pos2);
                            break;

                    }
                    factory.restore(plugin, center);
                    Factory.getFactories().add(factory);
                }

            }
        }.runTaskLater(plugin, 10L);
    }

    public boolean tooClose(Location location){
        for (Factory factory : Factory.getFactories()){

            if (factory.getCuboid() != null){
                if (factory.getCuboid().getLowerNE().distance(location) < 40){
                    return true;
                }
            }

        }
        return false;
    }

}
