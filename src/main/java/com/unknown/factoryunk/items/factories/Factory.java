package com.unknown.factoryunk.items.factories;

import com.google.common.collect.Lists;
import com.unknown.factoryunk.exceptions.FactoryException;
import com.unknown.factoryunk.gui.OrbInventory;
import com.unknown.factoryunk.gui.OrbInventoryExitItem;
import com.unknown.factoryunk.gui.OrbInventoryItem;
import com.unknown.factoryunk.gui.listeners.OrbInventoryListener;
import com.unknown.factoryunk.hologram.HologramLines;
import com.unknown.factoryunk.items.blueprint.Blueprint;
import com.unknown.factoryunk.items.blueprint.FactoryType;
import com.unknown.factoryunk.items.factories.types.CommonFactory;
import com.unknown.factoryunk.items.factories.types.LegendaryFactory;
import com.unknown.factoryunk.utils.*;
import lombok.Data;
import lombok.Getter;
import lombok.ToString;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.*;

@Data
@ToString
public class Factory implements Blueprint {

    // Factory core
    private UUID owner;
    private Set<UUID> admins;
    private Material material;
    private HologramLines lines;
    private long created;
    private FactoryType type;

    // Factory build
    private Cuboid cuboid;
    private Location center;
    private Location pos1;
    private Location pos2;
    private Location blockLocation;

    // Factory info
    private int health;
    private long lastDrop; // start to 0L
    private long delayInMillis = 5000L; // delay for the reward, 5000L is the default value
    private int factoryItemsAmount = 64 * 5; // TODO: implement different types of factory storage
    private int itemsCollected = 0;

    public static final int FACTORY_MAX_HEALTH = 200;

    /*
        Used to cache all the factories
     */
    @Getter
    private static Set<Factory> factories = new HashSet<>();

    /**
     * Restore constructor
     * @see #Factory(Material, FactoryType, UUID, Set, int)
     */
    public Factory(long created, UUID owner, Location blockLocation, Set<UUID> admins, Material material, int health, FactoryType type, Location center, Location pos1, Location pos2) {
        this.owner = owner;
        this.created = created;
        this.blockLocation = blockLocation;
        this.admins = admins;
        this.material = material;
        this.health = health;
        this.type = type;
        this.center = center;
        this.pos1 = pos1;
        this.pos2 = pos2;
        this.cuboid = new Cuboid(this.pos1, this.pos2);
    }

    /**
     * Needs to be called for each factory
     *
     * @param material the reward
     * @param type     of the factory(blueprint)
     * @param owner    who created the factory
     * @param admins   they can access to the factory
     * @throws FactoryException if the material given is not valid
     */
    public Factory(Material material, FactoryType type, UUID owner, Set<UUID> admins, int health) {
        if (!type.isValid(material))
            throw new FactoryException(material + " is not a valid type for " + type.name() + " Factory");
        this.created = System.currentTimeMillis();
        this.material = material;
        this.type = type;
        this.owner = owner;
        this.admins = admins;
        this.health = health;
    }

    public Factory(Material material, FactoryType type) {
        this(material, type, null, new HashSet<>(), -1);
    }

    public Factory() {
    }

    public void restore(Plugin plugin, Location center){

        HologramLines hologramLines = new HologramLines(
                ChatColor.GOLD + "Factory " + ChatColor.AQUA + StringUtils.firstUpper(material.name().toLowerCase()),
                "QUERY -- <" + material.name() + "> -- QUERY",
                ChatColor.GREEN + "Health: " + getHealthPercentage()
        );

        hologramLines.show(plugin, center, 0.4D);

        this.lines = hologramLines;
    }

    public void build(Block block, boolean store, Plugin plugin) {
        if (owner == null) throw new FactoryException("Owner cannot be null");

        this.blockLocation = block.getLocation();

        Schematic schematic = SchematicLoader.loadSchematic(plugin, "online");
        List<Location> locations = new ArrayList<>();

        for(int x = 0; x < schematic.getWidth(); ++x){
            for(int y = 0; y < schematic.getHeight(); ++y){
                for(int z = 0; z < schematic.getLength(); ++z){
                    int index = y * schematic.getWidth() * schematic.getLength() + z * schematic.getWidth() + x;
                    int b = schematic.getBlocks()[index] & 0xFF;//make the block unsigned,
                    // so that blocks with an id over 127, like quartz and emerald, can be pasted
                    Material m = Material.getMaterial(b);
                    Location location = block.getLocation().clone().add(x, y, z);
                    locations.add(location);
                    Block replace = location.getBlock();
                    replace.setType(m);
                }
            }
        }

        Location pos1 = null; // X MINIMA Z MASSIMA Y MINIMA
        Location pos2 = null; // X MASSIMA Z MINIMA Y MASSIMA

        int minY = -1;
        int maxY = -1;
        int minX = -1;
        int maxX = -1;
        int minZ = -1;
        int maxZ = -1;

        // just getting min/max Y
        for (Location loc : locations){
            if (minY == -1) minY = loc.getBlockY();
            if (maxY == -1) maxY = loc.getBlockY();
            if (minX == -1) minX = loc.getBlockX();
            if (maxX == -1) maxX = loc.getBlockX();
            if (minZ == -1) minZ = loc.getBlockZ();
            if (maxZ == -1) maxZ = loc.getBlockZ();


            if  (loc.getBlockY() < minY) minY = loc.getBlockY();
            if  (loc.getBlockY() > maxY) maxY = loc.getBlockY();

            if  (loc.getBlockX() < minX) minX = loc.getBlockX();
            if  (loc.getBlockX() > maxX) maxX = loc.getBlockX();

            if  (loc.getBlockZ() < minZ) minZ = loc.getBlockZ();
            if  (loc.getBlockZ() > maxZ) minZ = loc.getBlockZ();


        }

        pos1 = new Location(block.getWorld(), minX, minY, minZ);
        pos2 = new Location(block.getWorld(), maxX, maxY, maxZ);

        int centerX = schematic.getWidth() / 2;
        int centerY = schematic.getHeight() / 2 - 8;
        int centerZ = schematic.getLength() / 2;

        Location center = block.getLocation().clone().add(centerX, centerY, centerZ);

        if (store){
            String path = "factories." + created + ".";
            FileConfiguration fileConfiguration = YamlConfig.getConfiguration(plugin, "factories");
            fileConfiguration.set(path + "owner", owner.toString());
            fileConfiguration.set(path + "placed", LocationUtil.convertToString(blockLocation));
            fileConfiguration.set(path + "center", LocationUtil.convertToString(center));
            fileConfiguration.set(path + "pos1", LocationUtil.convertToString(pos1));
            fileConfiguration.set(path + "pos2", LocationUtil.convertToString(pos2));
            fileConfiguration.set(path + "material", material.name());
            fileConfiguration.set(path + "lastHealth", FACTORY_MAX_HEALTH);
            fileConfiguration.set(path + "type", type.name());
            fileConfiguration.set(path + "collectedItems", itemsCollected);
            fileConfiguration.set(path + "admins", new ArrayList<>());
            YamlConfig.saveConfig(plugin, fileConfiguration, "factories");
        }

        this.health = FACTORY_MAX_HEALTH;
        HologramLines hologramLines = new HologramLines(
                ChatColor.GOLD + "Factory " + ChatColor.AQUA + StringUtils.firstUpper(material.name().toLowerCase()),
                "QUERY -- <" + material.toString() +  "> -- QUERY",
                ChatColor.GREEN + "Health: " + getHealthPercentage()
        );

        hologramLines.show(plugin, center, 0.4D);
        this.lines = hologramLines;
        this.pos1 = pos1;
        this.pos2 = pos2;
        this.cuboid = new Cuboid(this.pos1, this.pos2);

        factories.add(this);


    }

    public boolean canDrop(){
        return System.currentTimeMillis()-lastDrop >= delayInMillis;
    }

    public void reduceHealth(int amount){
        this.health -= amount;
    }

    @Override
    public ItemStack generateBluePrint() {
        return new ItemFall(new ItemStack(Material.PAPER))
                .name(ChatColor.BLUE + "Blueprint " + ChatColor.GRAY + "(" + StringUtils.firstUpper(getReward().getType().name().toLowerCase()) + ")");
    }

    @Override
    public ItemStack getReward() {
        return new ItemStack(this.material);
    }

    @Override
    public FactoryType getType() {
        return this.type;
    }

    public String getHealthPercentage(){
        // 200 : 100 = health : x
        System.out.println(health);
        return ((health * 100) / 200) + "%";
    }

    /**
     * Gets factory from location
     *
     * @param location of the factory
     * @return the factory, can be null
     */
    public static Factory fromLocation(Location location) {
        return factories.stream()
                .filter(factory -> factory.getCuboid() != null && factory.getCuboid().contains(location))
                .findAny()
                .orElse(null);
    }

    public void addAdmin(Plugin plugin, UUID uuid){
        this.admins.add(uuid);
        YamlConfig.fastModify(plugin, "factories", "factories."+created+".admins", new ArrayList<>(this.admins));
    }

    public Inventory getInventory(){


        // Main Inventory

        OrbInventory inventory = new OrbInventory(StringUtils.firstUpper(material.name().toLowerCase()) + "'s Factory", 34, "factory", true, true);

        inventory.setItem(new OrbInventoryItem(
                new ItemFall(new ItemStack(material))
                .name(ChatColor.GOLD + "Factory Infos")
                .lore(Lists.newArrayList(
                        ChatColor.GRAY + "Type: " + ChatColor.AQUA + StringUtils.firstUpper(type.toString().toLowerCase()),
                        ChatColor.GRAY + "Reward Item: " + ChatColor.AQUA + StringUtils.firstUpper(material.toString().toLowerCase()),
                        ChatColor.GRAY + "Items/second: " + ChatColor.AQUA + "1/" + (delayInMillis / 1000),
                        ChatColor.GRAY + "Health: " + ChatColor.AQUA + getHealthPercentage(),
                        ChatColor.GRAY + "Status: " + ChatColor.AQUA + (this.health > 0 ? "Online" : "Broken")
                )), "infos", 5, 2
        ));

        inventory.setItem(new OrbInventoryItem(
                new ItemFall(new ItemStack(Material.CHEST))
                        .name(ChatColor.GOLD + "Factory storage")
                        .lore(Lists.newArrayList(
                                ChatColor.GRAY + "Size: " + ChatColor.AQUA + itemsCollected + "/" + factoryItemsAmount,
                                ChatColor.GRAY + "Items type: " + ChatColor.AQUA + StringUtils.firstUpper(material.name().toLowerCase())
                        )), "storage", 7, 2
        ));

        inventory.setItem(new OrbInventoryItem(
                new ItemFall(new ItemStack(Material.GOLDEN_CARROT))
                        .name(ChatColor.GOLD + "Factory workers")
                        .lore(Lists.newArrayList(
                                ChatColor.GRAY + "Click to manage your factory workers"
                        )), "settings", 7, 2
        ));

        inventory.setItem(new OrbInventoryExitItem(
                new ItemFall(Material.BARRIER, ChatColor.RED + "Exit", 1, (short)0,new ArrayList<>())
        ,"exit", 9, 4));


        inventory.setListener(new OrbInventoryListener() {
            @Override
            public void onClick(InventoryClickEvent event, Player player, OrbInventory inventory, OrbInventoryItem item) {

            }

            @Override
            public void onRightClick(InventoryClickEvent event, Player player, OrbInventory inventory, OrbInventoryItem item) {

            }

            @Override
            public void onLeftClick(InventoryClickEvent event, Player player, OrbInventory inventory, OrbInventoryItem item) {

            }

            @Override
            public void onClose(Player player, OrbInventory inventory) {

            }

            @Override
            public void onOpen(InventoryOpenEvent event, Player player, OrbInventory inventory) {

            }
        });

        return inventory.getInventory();
    }

    /**
     * Called every time a factory find an item, should be different for each factory type
     */
    public void reward(){
        //EMPTY FUNCTION
    }

}
