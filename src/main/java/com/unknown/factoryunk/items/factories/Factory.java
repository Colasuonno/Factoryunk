package com.unknown.factoryunk.items.factories;

import com.google.common.collect.Lists;
import com.unknown.factoryunk.FactoryUnk;
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
import com.unknown.factoryunk.items.factories.types.RareFactory;
import com.unknown.factoryunk.items.factories.workers.FactoryWorker;
import com.unknown.factoryunk.utils.*;
import lombok.Data;
import lombok.Getter;
import lombok.ToString;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.trait.Trait;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

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
    private List<FactoryWorker> workers = new ArrayList<>();

    // Factory build
    private Cuboid cuboid;
    private Location center;
    private Location pos1;
    private Location pos2;
    private Location blockLocation;
    private Plugin localPlugin; // TOTAL-UNSAFE

    // Factory info
    private String lastSchem = "";
    private int health;
    private long lastDrop; // start to 0L
    private long delayInMillis = 5000L; // delay for the reward, 5000L is the default value
    private int factoryItemsAmount = 64 * 5; // TODO: implement different types of factory storage
    private int itemsCollected = 0;

    // Econ infos
    private final int unitCost = 1;

    // Constants
    public static final int COMMON_FACTORY_MAX_HEALTH = 100;
    public static final int RARE_FACTORY_MAX_HEALTH = 200;
    public static final int LEGENDARY_FACTORY_MAX_HEALTH = 400;


    /*
        Used to cache all the factories
     */
    @Getter
    private static List<Factory> factories = new ArrayList<>();

    /**
     * Restore constructor
     *
     * @see #Factory(Material, FactoryType, UUID, Set, int)
     */
    public Factory(Plugin plugin, String lastSchem, int itemsCollected, long created, UUID owner, Location blockLocation, Set<UUID> admins, Material material, int health, FactoryType type, Location center, Location pos1, Location pos2) {
        this.localPlugin = plugin;
        this.itemsCollected = itemsCollected;
        this.lastSchem = lastSchem;
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
     */
    public Factory(Material material, FactoryType type, UUID owner, Set<UUID> admins, int health) {
        this.created = System.currentTimeMillis();
        this.material = material;
        this.type = type;
        this.owner = owner;
        this.admins = admins;
        this.health = health;
        this.lastSchem = "online";
    }

    public Factory(Material material, FactoryType type) {
        this(material, type, null, new HashSet<>(), -1);
    }

    /**
     * Needed Empty Constructor
     */
    public Factory() {
    }

    /**
     * Restore the holograms of the factory
     *
     * @param plugin instance
     * @param center of the factory
     */
    public void restore(Plugin plugin, Location center) {



        HologramLines hologramLines = new HologramLines(
                ChatColor.GOLD + "Factory " + ChatColor.AQUA + (isMoneyReward() ? "Money" : StringUtils.firstUpper(material.name().toLowerCase())),
                "QUERY -- <" + (isMoneyReward() ? Material.PAPER.toString() : material.toString()) + "> -- QUERY",
                health == 0 ? ChatColor.GRAY + "Status: " + ChatColor.RED + "Broken" : ChatColor.GREEN + "Health: " + getHealthPercentage()
        );

        hologramLines.show(plugin, center, 0.4D);

        this.lines = hologramLines;

    }

    /**
     * CREATE - method, to build and store the factory
     *
     * @param block  location placed
     * @param store  if you want to store the factory
     * @param plugin instance
     */
    public void build(Block block, boolean store, Plugin plugin) {
        if (owner == null) throw new FactoryException("Owner cannot be null");

        this.localPlugin = plugin;
        this.blockLocation = block.getLocation();

        Schematic schematic = SchematicLoader.loadSchematic(plugin, "online");
        List<Location> locations = new ArrayList<>();

        for (int x = 0; x < schematic.getWidth(); ++x) {
            for (int y = 0; y < schematic.getHeight(); ++y) {
                for (int z = 0; z < schematic.getLength(); ++z) {
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
        for (Location loc : locations) {
            if (minY == -1) minY = loc.getBlockY();
            if (maxY == -1) maxY = loc.getBlockY();
            if (minX == -1) minX = loc.getBlockX();
            if (maxX == -1) maxX = loc.getBlockX();
            if (minZ == -1) minZ = loc.getBlockZ();
            if (maxZ == -1) maxZ = loc.getBlockZ();


            if (loc.getBlockY() < minY) minY = loc.getBlockY();
            if (loc.getBlockY() > maxY) maxY = loc.getBlockY();

            if (loc.getBlockX() < minX) minX = loc.getBlockX();
            if (loc.getBlockX() > maxX) maxX = loc.getBlockX();

            if (loc.getBlockZ() < minZ) minZ = loc.getBlockZ();
            if (loc.getBlockZ() > maxZ) minZ = loc.getBlockZ();


        }

        pos1 = new Location(block.getWorld(), minX, minY, minZ);
        pos2 = new Location(block.getWorld(), maxX, maxY, maxZ);

        int centerX = schematic.getWidth() / 2;
        int centerY = schematic.getHeight() / 2 - 8;
        int centerZ = schematic.getLength() / 2;

        Location center = block.getLocation().clone().add(centerX, centerY, centerZ);

        if (store) {
            String path = "factories." + created + ".";
            FileConfiguration fileConfiguration = YamlConfig.getConfiguration(plugin, "factories");
            fileConfiguration.set(path + "owner", owner.toString());
            fileConfiguration.set(path + "placed", LocationUtil.convertToString(blockLocation));
            fileConfiguration.set(path + "center", LocationUtil.convertToString(center));
            fileConfiguration.set(path + "pos1", LocationUtil.convertToString(pos1));
            fileConfiguration.set(path + "pos2", LocationUtil.convertToString(pos2));
            fileConfiguration.set(path + "material", material.name());
            fileConfiguration.set(path + "lastHealth", COMMON_FACTORY_MAX_HEALTH);
            fileConfiguration.set(path + "type", type.name());
            fileConfiguration.set(path + "lastSchem", "online");
            fileConfiguration.set(path + "collectedItems", itemsCollected);
            fileConfiguration.set(path + "admins", new ArrayList<>());
            fileConfiguration.set(path + "workers", new HashMap<>());
            YamlConfig.saveConfig(plugin, fileConfiguration, "factories");
        }

        this.center = center;
        this.health = COMMON_FACTORY_MAX_HEALTH;
        HologramLines hologramLines = new HologramLines(
                ChatColor.GOLD + "Factory " + ChatColor.AQUA + (isMoneyReward() ? "Money" : StringUtils.firstUpper(material.name().toLowerCase())),
                "QUERY -- <" + (isMoneyReward() ? Material.PAPER.toString() : material.toString()) + "> -- QUERY",
                ChatColor.GREEN + "Health: " + getHealthPercentage()
        );

        hologramLines.show(plugin, center, 0.4D);
        this.lines = hologramLines;
        this.pos1 = pos1;
        this.pos2 = pos2;
        this.cuboid = new Cuboid(this.pos1, this.pos2);

        factories.add(this);


    }

    /**
     * Checks if a player can access the factory
     * @param uuid of the player
     * @return if he's allowed
     */
    public boolean isAllowed(UUID uuid){
        return owner.equals(uuid) || admins.contains(uuid);
    }

    /**
     * Store new worker, bypassing the MAX=2, can be called anywhere
     */
    public void storeNewWorker() {

        FileConfiguration fileConfiguration = YamlConfig.getConfiguration(localPlugin, "factories");


        NPC npc = CitizensAPI.getNPCRegistry().createNPC(EntityType.PLAYER, "NPC Worker");

      /*  npc.data().setPersistent(NPC.PLAYER_SKIN_TEXTURE_PROPERTIES_METADATA, "yJ0aW1lc3RhbXAiOjE1NjM4OTQwMDcxNDEsInByb2ZpbGVJZCI6IjZmM2ZlYThmNzBhMjQ3NmI5NjUyNjY4OGY4ZGJhNmJkIiwicHJvZmlsZU5hbWUiOiJXb2x0aCIsInNpZ25hdHVyZVJlcXVpcmVkIjp0cnVlLCJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOWRmYzZmMTcyYmI4ZmRjZTJjMmVkY2NlYzJjNWFlYjAyNmJhNTMxMDFhNDJkOWExMzc3MmNkNWY4ZTc1NjM1MSJ9fX0=");
        npc.data().setPersistent(NPC.PLAYER_SKIN_TEXTURE_PROPERTIES_SIGN_METADATA, "kAflQsJdIjh20dvddlTT4fDGPYESfF1S4wZcOvFEesf0oa8UHmHBAGylQ4cUXVl/czphJo3wrE67c1TLfGzO3+qwvJnbuskED76qzGnK2VwkYxIQi77V/vuD4zj1RLsAQ0Gqh6vUEcHuRW7HmFyLPn8llQ76iEZCmbsrgE2/j7G+oDqAivZt6US6iz+MzZS7RWp6/B4yw2lW9vwMQB9BWYbysWgryptzcIKLc844AWFTZceS6jTEJFdddqHmWCfJk4clpEgI0WxQBFsIrA0eG/bWLV4GdUt+fVtKY7sMJgWHP1WkpXWXG5WTp5nDSnLi8hnNf+51sJnsKv5XgP55S9f0Hw4laVNXFNo9B81yXxMgGN/e7nV5rD9lhpzBX31Bp0QSaOESNtXiZ06j1KjPULKVOpynNzeU595m2edn/kk5dC6tVCqSIGFmtCdhwkrWG7WvxXhBhtIbLYRJnaGdyFK/eQYRKhcbu3RfQUGKtKUmCnC36OEL3c4Wdr8nAWsXJYGx/YGBzJvj026d/v1q0jQnIOiwXd5puf2J94yye+ijXdKvVwpKElRPkDZp1rxebnrGIa18eaBomxUoFkb7wI1zIxU8pNVDdS6LvWcsD/dmEa0qhXuJjZLEEg/rvlQcKdVf6YX6oRVAjTl6hniBcwPlkcUn8FUyuK4bv/7hHSg=");
        npc.data().setPersistent(NPC.RESPAWN_DELAY_METADATA, 1);*/
        npc.setProtected(true);

        npc.spawn(center);
        String path = "factories." + created + ".workers." + npc.getId() + ".";
        fileConfiguration.set(path + "collectedItems", 0);
        YamlConfig.saveConfig(localPlugin, fileConfiguration, "factories");

        FactoryWorker worker = new FactoryWorker(npc.getId(), 0);
        workers.add(worker);


    }

    /**
     * Save the last health,collectedItems into the config
     *
     * @param plugin
     */
    public void save(Plugin plugin) {

        String path = "factories." + created + ".";
        FileConfiguration fileConfiguration = YamlConfig.getConfiguration(plugin, "factories");

        fileConfiguration.set(path + "lastHealth", this.health);
        fileConfiguration.set(path + "collectedItems", this.itemsCollected);

        for (FactoryWorker worker : workers) {
            fileConfiguration.set(path + "workers." + worker.getNPC().getId() + ".collectedItems", worker.getCollectedItems());
        }

        YamlConfig.saveConfig(plugin, fileConfiguration, "factories");

    }

    /**
     * Gets the sum of all the factory workers item
     * @return the total amount of factory workers collected items
     */
    public int getWorkersItemsAmount() {
        int amount = 0;
        for (FactoryWorker worker : workers) {
            amount += worker.getCollectedItems();
        }
        return amount;
    }

    /**
     * ABSOLUTE remove a factory, from game and storage, (Holos and npcs)
     */
    public void destroy(){

        for (Block block : this.cuboid){
            block.setType(Material.AIR);
        }

        for (FactoryWorker worker : workers){
            worker.getNPC().despawn();
            worker.getNPC().destroy();
        }

        lines.destroy();
        YamlConfig.fastModify(localPlugin, "factories", "factories."+created, null);
        Factories.createdRemove(created);

    }

    /**
     * Modify the state and the building of the factory
     *
     * @param plugin instance
     * @param old    old state
     * @param state  changed stated
     * @throws FactoryException if the old and the state are the same
     */
    private void setState(Plugin plugin, String old, String state) {

        if (old.equalsIgnoreCase(state)) throw new FactoryException("old cannot be the same to state");

        Location center = blockLocation;

        Schematic oldSchem = SchematicLoader.loadSchematic(plugin, old);
        Schematic schematic = SchematicLoader.loadSchematic(plugin, state);
        for (int x = 0; x < oldSchem.getWidth(); ++x) {
            for (int y = 0; y < oldSchem.getHeight(); ++y) {
                for (int z = 0; z < oldSchem.getLength(); ++z) {
                    Location location = center.clone().add(x, y, z);
                    Block replace = location.getBlock();
                    replace.setType(Material.AIR);
                }
            }
        }

        new BukkitRunnable() {
            @Override
            public void run() {

                if (state.equalsIgnoreCase("offline")){
                    lines.modify("Health", ChatColor.GRAY + "Status: " + ChatColor.RED + "Broken", ArmorStand.class);
                } else {
                    lines.modify("Status", ChatColor.GREEN + "Health: " + getHealthPercentage(), ArmorStand.class);
                }

                for (int x = 0; x < schematic.getWidth(); ++x) {
                    for (int y = 0; y < schematic.getHeight(); ++y) {
                        for (int z = 0; z < schematic.getLength(); ++z) {
                            int index = y * schematic.getWidth() * schematic.getLength() + z * schematic.getWidth() + x;
                            int b = schematic.getBlocks()[index] & 0xFF;//make the block unsigned,
                            // so that blocks with an id over 127, like quartz and emerald, can be pasted
                            Material m = Material.getMaterial(b);
                            Location location = center.clone().add(x, y, z);
                            Block replace = location.getBlock();
                            replace.setType(m);
                        }
                    }
                }

            }
        }.runTaskLater(plugin, 5L);
    }

    /**
     * Get max health by factory param
     *
     * @param type of the factory
     * @return the max health
     */
    private int getMaxHealth(FactoryType type) {
        switch (type) {
            case RARE:
                return RARE_FACTORY_MAX_HEALTH;
            case COMMON:
                return COMMON_FACTORY_MAX_HEALTH;
            case LEGENDARY:
                return LEGENDARY_FACTORY_MAX_HEALTH;
            default:
                return -1;
        }
    }

    /**
     * Get max health by factory type
     *
     * @return the max health
     */
    public int getMaxHealth() {

        switch (type) {
            case RARE:
                return RARE_FACTORY_MAX_HEALTH;
            case COMMON:
                return COMMON_FACTORY_MAX_HEALTH;
            case LEGENDARY:
                return LEGENDARY_FACTORY_MAX_HEALTH;
            default:
                return -1;
        }

    }

    /**
     * Check is a factory is not full, and can collect an item
     *
     * @return if a factory is ready to collect an item
     */
    public boolean canDrop() {

        String lastSchem = getLastSchem();

        if (this.health == 0 && getLastSchem().equalsIgnoreCase("online")) {
            this.setLastSchem("offline");
            setState(localPlugin, lastSchem, this.lastSchem);
            YamlConfig.fastModify(localPlugin, "factories", "factories." + created + ".lastSchem", "offline");
        } else if (this.health > 0 && getLastSchem().equalsIgnoreCase("offline")) {
            this.setLastSchem("online");
            setState(localPlugin, lastSchem, this.lastSchem);
            YamlConfig.fastModify(localPlugin, "factories", "factories." + created + ".lastSchem", "online");
        }

        return this.itemsCollected < this.factoryItemsAmount && this.health > 0 && System.currentTimeMillis() - lastDrop >= delayInMillis;
    }

    /**
     * If the health is greater than 0 it will reduce the health of param given
     *
     * @param amount reduce amount
     */
    public void reduceHealth(int amount) {
        if (health > 0) this.health -= amount;
    }

    /**
     * Collect an item bypassing any limits
     *
     * @param amount of item to collect
     */
    public void collect(int amount) {
        this.itemsCollected += amount;
    }

    @Override
    public ItemStack generateBluePrint() {
        return new ItemFall(new ItemStack(Material.PAPER))
                .name(ChatColor.BLUE + "Blueprint " + ChatColor.GRAY + "(" + (isMoneyReward() ? "Money" : StringUtils.firstUpper(getReward().getType().name().toLowerCase())) + ")");
    }

    @Override
    public ItemStack getReward() {
        return new ItemStack(this.material);
    }

    @Override
    public FactoryType getType() {
        return this.type;
    }

    public boolean isMoneyReward() {
        return material.equals(Material.RECORD_10);
    }

    /**
     * Gets the current health %
     *
     * @return
     */
    public String getHealthPercentage() {
        // 200 : 100 = health : x
        return ((health * 100) / getMaxHealth()) + "%";
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

    /**
     * Gets factory from uuid
     *
     * @param uuid of the owner
     * @return the factory, can be null
     */
    @Deprecated
    public static Factory fromOwner(UUID uuid) {
        return factories.stream()
                .filter(factory -> factory.getOwner() != null)
                .findAny()
                .orElse(null);
    }

    /**
     * Add factory admin (perms)
     *
     * @param uuid of the player
     */
    public void addAdmin(UUID uuid) {
        this.admins.add(uuid);
        List<String> result = new ArrayList<>();
        for (UUID admins : admins){
            result.add(admins.toString());
        }
        YamlConfig.fastModify(localPlugin, "factories", "factories." + created + ".admins", result);
    }

    /**
     * Repair the factory by param given
     * WARNING: No secure check is made to this function,
     * you can destroy some factory from over sized repair amount
     *
     * @param health amount of health to repair
     */
    public void repair(int health) {
        this.health += health;
    }


    /**
     * Called every time a factory find an item, should be different for each factory type
     */
    public void reward() {
        //EMPTY FUNCTION
    }


    /**
     * Upgrade the factory to the next tier
     *
     * @return if the upgrade was successful
     */
    public boolean upgradeFactory() {

        boolean max = nextStringTier().equalsIgnoreCase("Max");

        if (max) return false;
        else {

            FactoryType newz = nextTier();
            if (newz != null) {
                Factory newFactoryInstance;

                switch (newz) {
                    case LEGENDARY:
                        newFactoryInstance = new LegendaryFactory(localPlugin, lastSchem, itemsCollected, created, owner, blockLocation, new HashSet<>(), material, getMaxHealth(newz), newz, center, pos1, pos2);
                        break;
                    case RARE:
                        newFactoryInstance = new RareFactory(localPlugin, lastSchem, itemsCollected, created, owner, blockLocation, new HashSet<>(), material, getMaxHealth(newz), newz, center, pos1, pos2);
                        break;
                    default:
                        return false;

                }

                newFactoryInstance.setWorkers(workers);
                newFactoryInstance.setAdmins(admins);

                lines.destroy();
                YamlConfig.fastModify(localPlugin, "factories", "factories." + created + ".type", newz.toString());
                Factories.createdRemove(created);
                factories.add(newFactoryInstance);
                newFactoryInstance.restore(localPlugin, center);

                return true;
            } else return false;

        }

    }

    /**
     * Gets the next factory tier
     *
     * @return the next factory tier
     */
    private FactoryType nextTier() {
        switch (type) {
            case RARE:
                return FactoryType.LEGENDARY;
            case COMMON:
                return FactoryType.RARE;
            case LEGENDARY:
            default:
                return null;
        }
    }

    /**
     * Gets the next String Factory tier
     *
     * @return the next factory tier, if null returns 'Max'
     */
    private String nextStringTier() {
        FactoryType type = nextTier();
        if (type == null) return "Max";
        else return StringUtils.firstUpper(type.name().toLowerCase());
    }


    /*

         Factory Inventory section

        INFO: All inventories are unique and rebuild each time, if you want to keep them in cache
        just put false to destroy field in OrbInventory

     */

    /**
     * Gets the main factory inventory
     *
     * @return the main inventory
     */
    public Inventory getInventory() {


        // Main Inventory

        OrbInventory inventory = new OrbInventory((isMoneyReward() ? "Money" : StringUtils.firstUpper(material.name().toLowerCase())) + "'s Factory", 27, "factory", true, true);

        inventory.setItem(new OrbInventoryItem(
                new ItemFall(new ItemStack((isMoneyReward() ? Material.PAPER : material)))
                        .name(ChatColor.GOLD + "Factory Infos")
                        .lore(Lists.newArrayList(
                                ChatColor.GRAY + "Type: " + ChatColor.AQUA + StringUtils.firstUpper(type.toString().toLowerCase()),
                                ChatColor.GRAY + "Reward Item: " + ChatColor.AQUA + (isMoneyReward() ? "Money" : StringUtils.firstUpper(material.toString().toLowerCase())),
                                ChatColor.GRAY + "Items/second: " + ChatColor.AQUA + "1/" + (delayInMillis / 1000),
                                ChatColor.GRAY + "Health: " + ChatColor.AQUA + getHealthPercentage(),
                                ChatColor.GRAY + "Status: " + ChatColor.AQUA + (this.health > 0 ? "Online" : "Broken"),
                                ChatColor.GRAY + "Collected Items: " + ChatColor.AQUA + itemsCollected + "/" + factoryItemsAmount,
                                ChatColor.GRAY + "Factory workers Collected Items: " + ChatColor.AQUA + getWorkersItemsAmount() + "/" + (FactoryWorker.STORAGE_AMOUNT * workers.size()),
                                "",
                                ChatColor.RED + "Click to withraw all the collected items storable in your inventory"
                        )), "infos", 5, 2
        ));

        boolean max = nextStringTier().equalsIgnoreCase("Max");

        inventory.setItem(new OrbInventoryItem(
                new ItemFall(new ItemStack(Material.CACTUS))
                        .name(ChatColor.GREEN + "Upgrade your factory")
                        .lore(Lists.newArrayList(
                                ChatColor.GRAY + "Upgrade to " + ChatColor.AQUA + nextStringTier(),
                                ChatColor.GRAY + "Cost: " + ChatColor.AQUA + (max ? "N/D" : "$" + nextTier().getCost()),
                                "",
                                ChatColor.GRAY + "Click to instant upgrade your factory"
                        )), "upgrade", 1, 3
        ));

     /*   inventory.setItem(new OrbInventoryItem(
                new ItemFall(new ItemStack(Material.CHEST))
                        .name(ChatColor.GOLD + "Factory storage")
                        .lore(Lists.newArrayList(
                                ChatColor.GRAY + "Size: " + ChatColor.AQUA + itemsCollected + "/" + factoryItemsAmount,
                                ChatColor.GRAY + "Items type: " + ChatColor.AQUA + StringUtils.firstUpper(material.name().toLowerCase())
                        )), "storage", 7, 2
        ));*/

        inventory.setItem(new OrbInventoryItem(
                new ItemFall(new ItemStack(Material.GOLDEN_CARROT))
                        .name(ChatColor.GOLD + "Factory workers")
                        .lore(Lists.newArrayList(
                                ChatColor.GRAY + "Click to manage your factory workers"
                        )), "settings", 7, 2
        ));

        inventory.setItem(new OrbInventoryItem(
                new ItemFall(new ItemStack(Material.STICK))
                        .name(ChatColor.GOLD + "Maintenance")
                        .lore(Lists.newArrayList(
                                ChatColor.GRAY + "Click to manual maintenance your factory"
                        )), "maintenance", 3, 2
        ));

        inventory.setItem(new OrbInventoryExitItem(
                new ItemFall(Material.BARRIER, ChatColor.RED + "Exit", 1, (short) 0, new ArrayList<>())
                , "exit", 9, 3));


        inventory.setListener(new OrbInventoryListener() {
            @Override
            public void onClick(InventoryClickEvent event, Player player, OrbInventory inventory, OrbInventoryItem item) {
                if (item != null) {

                    // We need econ
                    FactoryUnk instance = (FactoryUnk) localPlugin;

                    switch (item.getMetaData()) {
                        case "infos":
                            int missed = 0;

                            int collect = itemsCollected;

                            if (isMoneyReward()) {

                                int total = itemsCollected + getWorkersItemsAmount();

                                EconomyResponse response = instance.getEconomy().depositPlayer(player, total);
                                if (response.transactionSuccess()){
                                    StringUtils.i("<e>${0} has been added to your account", player, total);
                                    itemsCollected = 0;
                                    for (FactoryWorker worker : workers){
                                        worker.setCollectedItems(0);
                                    }
                                }
                            } else {
                                for (int i = 0; i < (collect); i++) {
                                    if (PlayersUtil.canHoldItem(player, new ItemStack(material, 1))) {
                                        player.getInventory().addItem(new ItemStack(material, 1));
                                        itemsCollected--;
                                    } else missed++;
                                }

                                for (FactoryWorker worker : workers) {
                                    int coll = worker.getCollectedItems();
                                    for (int j = 0; j < coll; j++) {
                                        if (PlayersUtil.canHoldItem(player, new ItemStack(material, 1))) {
                                            player.getInventory().addItem(new ItemStack(material, 1));
                                            worker.setCollectedItems(worker.getCollectedItems() - 1);
                                        } else missed++;
                                    }
                                }
                            }

                            if (missed != 0)
                                StringUtils.e(missed + " items are still in the factory! Free your inventory and withraw them!", player);

                            player.closeInventory();
                            break;
                        case "settings":
                            player.closeInventory();
                            new BukkitRunnable() {
                                @Override
                                public void run() {
                                    player.openInventory(factoryWorkers());
                                }
                            }.runTaskLater(localPlugin, 1L);
                            break;
                        case "maintenance":
                            player.closeInventory();
                            // 1.8 close/open problem, we need a task
                            new BukkitRunnable() {
                                @Override
                                public void run() {
                                    player.openInventory(maintenance());
                                }
                            }.runTaskLater(localPlugin, 1L);
                            break;
                        case "upgrade":
                            player.closeInventory();
                            if (upgradeFactory()) {
                                StringUtils.i("<e>Factory upgraded successfully", player);
                            } else {
                                StringUtils.e("You cannot upgrade your factory anymore", player);
                            }
                        default:
                            break;
                    }
                }
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
     * Gets the maintenance inventory of the factory
     *
     * @return
     */
    private Inventory maintenance() {

        int[] usable = new int[]{20, 50, 100, 150, 200};

        OrbInventory inventory = new OrbInventory("Factory maintenance", 27, "maintenance", true, true);

        int x = 3;

        int maxHealth = getMaxHealth();

        for (int num : usable) {

            int effective = this.health + num > maxHealth ? maxHealth - this.health : num;

            inventory.setItem(new OrbInventoryItem(
                    new ItemFall(new ItemFall(new ItemStack(Material.BOOK)).name(ChatColor.GOLD + "+" + (num / 2) + "% Repair").lore(Lists.newArrayList(
                            ChatColor.GRAY + "Health: " + ChatColor.GREEN + this.getHealthPercentage(),
                            ChatColor.GRAY + "Repair: " + ChatColor.GREEN + ChatColor.GREEN + "+" + (((effective * 100) / 200)) + "% ",
                            ChatColor.GRAY + "Repair cost:" + ChatColor.GREEN + " $" + (effective * unitCost)
                    ))),
                    effective + "", x, 2
            ));
            x++;
        }

        inventory.setItem(new OrbInventoryItem(
                new ItemFall(Material.ARROW, ChatColor.RED + "Back to main inventory", 1, (short) 0, new ArrayList<>())
                , "back", 9, 3));

        inventory.setListener(new OrbInventoryListener() {
            @Override
            public void onClick(InventoryClickEvent event, Player player, OrbInventory inventory, OrbInventoryItem item) {
                if (item != null) {
                    switch (item.getMetaData()) {
                        case "back":
                            player.closeInventory();
                            new BukkitRunnable() {
                                @Override
                                public void run() {
                                    player.openInventory(getInventory());
                                }
                            }.runTaskLater(localPlugin, 2L);
                            break;
                        default:

                            if (StringUtils.isInt(item.getMetaData())) {
                                int value = Integer.parseInt(item.getMetaData());
                                repair(value);
                                StringUtils.i(ChatColor.GREEN + "Maintenance done! Current factory health: " + health + "/" + getMaxHealth() + " (" + getHealthPercentage() + ")", player);
                                getLines().modify("Health", ChatColor.GREEN + "Health: " + getHealthPercentage(), ArmorStand.class);
                                player.closeInventory();
                            }

                            break;
                    }
                }
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
     * Gets the factory workers inventory
     *
     * @return
     */
    private Inventory factoryWorkers() {

        OrbInventory inventory = new OrbInventory("Factory workers", 36, "workers", true, true);

        int x = 1;

        for (FactoryWorker worker : workers) {

            inventory.setItem(new OrbInventoryItem(
                    new ItemFall(new ItemFall(new ItemStack(Material.SKULL_ITEM)).name(ChatColor.GREEN + "Factory worker #" + worker.getId()).dur((short) 3).lore(Lists.newArrayList(
                            ChatColor.GRAY + "Storage: " + ChatColor.GREEN + worker.getCollectedItems() + "/" + FactoryWorker.STORAGE_AMOUNT
                    ))),
                    worker.getId() + "", x, 1
            ));
            x++;
        }

        inventory.setItem(new OrbInventoryItem(
                new ItemFall(Material.WOOL, ChatColor.RED + "Add a factory worker", 1, (short) 5, Lists.newArrayList(
                        ChatColor.GRAY + "Click to add a factory worker"
                ))
                , "add", 1, 4));

        inventory.setItem(new OrbInventoryItem(
                new ItemFall(Material.ARROW, ChatColor.RED + "Back to main inventory", 1, (short) 0, new ArrayList<>())
                , "back", 9, 4));

        inventory.setListener(new OrbInventoryListener() {
            @Override
            public void onClick(InventoryClickEvent event, Player player, OrbInventory inventory, OrbInventoryItem item) {
                if (item != null) {
                    switch (item.getMetaData()) {
                        case "back":
                            player.closeInventory();
                            new BukkitRunnable() {
                                @Override
                                public void run() {
                                    player.openInventory(getInventory());
                                }
                            }.runTaskLater(localPlugin, 2L);
                            break;
                        case "add":
                            if (workers.size() >= 2) {
                                player.closeInventory();
                                StringUtils.e("You have reached the maximum amount of factory workers", player);
                            } else {
                                storeNewWorker();
                                StringUtils.i("<e>You have now " + workers.size() + " workers", player);
                                player.closeInventory();
                            }
                        default:
                            break;
                    }
                }
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
}
