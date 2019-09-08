package com.unknown.factoryunk.items.factories;

import com.google.common.collect.Lists;
import com.sun.org.apache.regexp.internal.RE;
import com.unknown.factoryunk.gui.OrbInventory;
import com.unknown.factoryunk.gui.OrbInventoryItem;
import com.unknown.factoryunk.items.blueprint.FactoryType;
import com.unknown.factoryunk.items.factories.types.CommonFactory;
import com.unknown.factoryunk.items.factories.types.LegendaryFactory;
import com.unknown.factoryunk.items.factories.types.RareFactory;
import com.unknown.factoryunk.items.factories.workers.FactoryWorker;
import com.unknown.factoryunk.utils.LocationUtil;
import com.unknown.factoryunk.utils.StringUtils;
import com.unknown.factoryunk.utils.YamlConfig;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

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

        new BukkitRunnable() {
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
                    int collectedItems = fileConfiguration.getInt(path + "collectedItems");
                    String lastSchem = fileConfiguration.getString(path + "lastSchem");

                    Set<UUID> admins = new HashSet<>();

                    for (String ad : fileConfiguration.getStringList(path + "admins")) {
                        admins.add(UUID.fromString(ad));
                    }

                    Factory factory;

                    switch (type) {
                        case LEGENDARY:
                            factory = new LegendaryFactory(plugin, lastSchem, collectedItems, created, owner, placed, admins, material, health, type, center, pos1, pos2);
                            break;
                        case RARE:
                            factory = new RareFactory(plugin, lastSchem, collectedItems, created, owner, placed, admins, material, health, type, center, pos1, pos2);
                            break;
                        case COMMON:
                            factory = new CommonFactory(plugin, lastSchem, collectedItems, created, owner, placed, admins, material, health, type, center, pos1, pos2);
                            break;
                        default:
                            factory = new Factory(plugin, lastSchem, collectedItems, created, owner, placed, admins, material, health, type, center, pos1, pos2);
                            break;

                    }

                    List<NPC> npcs = new ArrayList<>();

                    if (fileConfiguration.get("factories." + section + ".workers") != null) {
                        for (String workers : fileConfiguration.getConfigurationSection("factories." + section + ".workers").getKeys(false)) {

                            int id = Integer.parseInt(workers);
                            int collectedNPCItems = fileConfiguration.getInt("factories." + section + ".workers." + workers + ".collectedItems");

                            FactoryWorker factoryWorker = new FactoryWorker(id, collectedNPCItems);

                            factory.getWorkers().add(factoryWorker);

                            NPC npc = factoryWorker.getNPC();
                            if (npc != null) {
                                if (npc.isSpawned()) npc.despawn();
                                npcs.add(npc);
                            }


                        }
                    }

                    if (!npcs.isEmpty()) {
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                if (npcs.isEmpty()) {
                                    cancel();
                                    return;
                                }
                                npcs.get(0).spawn(factory.getCenter());
                                npcs.remove(0);
                            }
                        }.runTaskTimer(plugin, 0L, 20L);
                    }

                    factory.restore(plugin, center);
                    Factory.getFactories().add(factory);
                }

            }
        }.runTaskLater(plugin, 10L);
    }

    /**
     * Remove a factory from date
     *
     * @param created date
     */
    public static void createdRemove(long created) {
        for (Factory cloned : new ArrayList<>(Factory.getFactories())) {
            if (cloned.getCreated() == created) {
                Factory.getFactories().remove(cloned);
            }
        }
    }

    /**
     * Checks if a location is too close to another
     *
     * @param location to check
     * @return if it is too close
     */
    public boolean tooClose(Location location) {
        for (Factory factory : Factory.getFactories()) {

            if (factory.getCuboid() != null) {
                if (location.getWorld().getName().equalsIgnoreCase(factory.getCenter().getWorld().getName()) && factory.getCuboid().getLowerNE().distance(location) < 30) {
                    return true;
                }
            }

        }
        return false;
    }

    /**
     * Gets all the factory from uuid
     * @param owner the uuid of the player
     * @return the inventory
     */
    public static Inventory getFactories(UUID owner) {

        OrbInventory orbInventory = new OrbInventory(Bukkit.getOfflinePlayer(owner).getName() + "'s Factories", 45, "factories", true, true);

        int x = 1;
        int y = 1;

        for (Factory factory : Factory.getFactories()) {
            if (factory.getOwner().equals(owner)) {
                ItemStack bluePrint = factory.generateBluePrint();
                ItemMeta meta = bluePrint.getItemMeta();

                meta.setLore(
                        Lists.newArrayList(
                                ChatColor.GRAY + "Reward: " + ChatColor.AQUA + (factory.isMoneyReward() ? "Money" : StringUtils.firstUpper(factory.getReward().getType().name().toLowerCase())),
                                ChatColor.GRAY + "Center: " + ChatColor.AQUA + LocationUtil.convertToString(factory.getCenter())
                                )
                );

                bluePrint.setItemMeta(meta);
                orbInventory.setItem(new OrbInventoryItem(
                        bluePrint,
                        "", x, y
                ));

                if (x == 9) {
                    y++;
                    x = 1;
                } else x++;

            }
        }

        return orbInventory.getInventory();
    }

}
