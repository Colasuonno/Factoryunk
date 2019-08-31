package com.unknown.factoryunk.items.factories;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.function.mask.ExistingBlockMask;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.transform.AffineTransform;
import com.sk89q.worldedit.math.transform.Transform;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.registry.WorldData;
import com.unknown.factoryunk.exceptions.FactoryException;
import com.unknown.factoryunk.hologram.HologramLines;
import com.unknown.factoryunk.items.blueprint.Blueprint;
import com.unknown.factoryunk.items.blueprint.FactoryType;
import com.unknown.factoryunk.utils.ItemFall;
import com.unknown.factoryunk.utils.LocationUtil;
import com.unknown.factoryunk.utils.StringUtils;
import com.unknown.factoryunk.utils.YamlConfig;
import lombok.Data;
import lombok.Getter;
import lombok.ToString;
import net.minecraft.server.v1_8_R3.Items;
import net.minecraft.server.v1_8_R3.NBTCompressedStreamTools;
import net.minecraft.server.v1_8_R3.NBTTagCompound;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.FileInputStream;
import java.net.URL;
import java.util.*;

@Data
@ToString
public class Factory implements Blueprint {

    private UUID owner;
    private Location blockLocation;
    private Set<UUID> admins;
    private Material material;
    private HologramLines lines;
    private int health;
    private FactoryType type;
    private Cuboid cuboid;
    private Location center;
    private Location pos1;
    private Location pos2;
    
    public static final int FACTORY_MAX_HEALTH = 100;

    /*
        Used to cache all the factories
     */
    @Getter
    private static Set<Factory> factories = new HashSet<>();

    /**
     * Restore constructor
     * @see #Factory(Material, FactoryType, UUID, Set, int)
     */
    public Factory(UUID owner, Location blockLocation, Set<UUID> admins, Material material, int health, FactoryType type, Location center, Location pos1, Location pos2) {
        this.owner = owner;
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
        this.material = material;
        this.type = type;
        this.owner = owner;
        this.admins = admins;
        this.health = health;
    }

    public Factory(Material material, FactoryType type) {
        this(material, type, null, new HashSet<>(), -1);
    }

    public Schematic loadSchematic(Plugin plugin, String name) {
        if (!name.endsWith(".schematic"))
            name = name + ".schematic";
        File file = new File(plugin.getDataFolder().getAbsolutePath() + "/schematics/online.schematic");
        if (!file.exists())
            return null;
        try {
            FileInputStream stream = new FileInputStream(file);
            NBTTagCompound nbtdata = NBTCompressedStreamTools.a(stream);

            short width = nbtdata.getShort("Width");
            short height = nbtdata.getShort("Height");
            short length = nbtdata.getShort("Length");

            byte[] blocks = nbtdata.getByteArray("Blocks");
            byte[] data = nbtdata.getByteArray("Data");

            byte[] addId = new byte[0];

            if (nbtdata.hasKey("AddBlocks")) {
                addId = nbtdata.getByteArray("AddBlocks");
            }

            short[] sblocks = new short[blocks.length];
            for (int index = 0; index < blocks.length; index++) {
                if ((index >> 1) >= addId.length) {
                    sblocks[index] = (short) (blocks[index] & 0xFF);
                } else {
                    if ((index & 1) == 0) {
                        sblocks[index] = (short) (((addId[index >> 1] & 0x0F) << 8) + (blocks[index] & 0xFF));
                    } else {
                        sblocks[index] = (short) (((addId[index >> 1] & 0xF0) << 4) + (blocks[index] & 0xFF));
                    }
                }
            }

            stream.close();
            return new Schematic(name, sblocks, data, width, length, height);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public void restore(Location center){

        HologramLines hologramLines = new HologramLines(
                ChatColor.GOLD + "Factory " + ChatColor.AQUA + StringUtils.firstUpper(material.name().toLowerCase()),
                "QUERY -- <DIAMOND> -- QUERY",
                ChatColor.GREEN + "Health: 100%"
        );

        hologramLines.show(center, 0.4D);

        this.lines = hologramLines;
    }

    public void build(Block block, boolean store, Plugin plugin) {
        if (owner == null) throw new FactoryException("Owner cannot be null");

        this.blockLocation = block.getLocation();

        Schematic schematic = loadSchematic(plugin, "online");
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
            String path = "factories." + System.currentTimeMillis() + ".";
            FileConfiguration fileConfiguration = YamlConfig.getConfiguration(plugin, "factories");
            fileConfiguration.set(path + "owner", owner.toString());
            fileConfiguration.set(path + "placed", LocationUtil.convertToString(blockLocation));
            fileConfiguration.set(path + "center", LocationUtil.convertToString(center));
            fileConfiguration.set(path + "pos1", LocationUtil.convertToString(pos1));
            fileConfiguration.set(path + "pos2", LocationUtil.convertToString(pos2));
            fileConfiguration.set(path + "material", material.name());
            fileConfiguration.set(path + "lastHealth", FACTORY_MAX_HEALTH);
            fileConfiguration.set(path + "type", type.name());
            YamlConfig.saveConfig(plugin, fileConfiguration, "factories");
        }

        this.health = FACTORY_MAX_HEALTH;

        HologramLines hologramLines = new HologramLines(
                ChatColor.GOLD + "Factory " + ChatColor.AQUA + StringUtils.firstUpper(material.name().toLowerCase()),
                "QUERY -- <DIAMOND> -- QUERY",
                ChatColor.GREEN + "Health: 100%"
        );

        hologramLines.show(center, 0.4D);
        this.lines = hologramLines;
        factories.add(this);


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

    /**
     * Gets factory from location
     *
     * @param location of the factory
     * @return the factory, can be null
     */
    public static Factory fromLocation(Location location) {
        return factories.stream()
                .filter(factory -> factory.getBlockLocation().equals(location))
                .findAny()
                .orElse(null);
    }

}
