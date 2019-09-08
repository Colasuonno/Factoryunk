package com.unknown.factoryunk.listener;

import com.massivecraft.factions.FLocation;
import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.FPlayers;
import com.massivecraft.factions.Faction;
import com.unknown.factoryunk.FactoryUnk;
import com.unknown.factoryunk.items.blueprint.FactoryType;
import com.unknown.factoryunk.items.factories.Factory;
import com.unknown.factoryunk.items.factories.FactoryItem;
import com.unknown.factoryunk.items.factories.types.CommonFactory;
import com.unknown.factoryunk.items.factories.types.LegendaryFactory;
import com.unknown.factoryunk.utils.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

import java.util.HashSet;

public class PlayerListener implements Listener {

    private FactoryUnk plugin;

    public PlayerListener(FactoryUnk plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlace(PlayerInteractEvent e) {

        FPlayer fPlayer = FPlayers.getInstance().getByPlayer(e.getPlayer());

        if (e.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
            if (fPlayer.isInOwnTerritory() && FactoryType.isValid(e.getItem())) {

                if (Integer.parseInt(fPlayer.getFactionId()) > 0) {

                    Block target = e.getPlayer().getTargetBlock((HashSet<Byte>) null, 10);
                    if (target == null) {
                        StringUtils.e("Location not valid", e.getPlayer());
                        return;
                    }

                    if (plugin.getFactories().tooClose(target.getLocation())) {
                        StringUtils.e("You are too close to a factory", e.getPlayer());
                        return;
                    }

                    FactoryItem factoryItem;
                    boolean money = e.getItem().getItemMeta().getDisplayName().contains("Money");
                    if (money) {
                        factoryItem = null;
                    } else factoryItem = new FactoryItem(e.getItem());

                    CommonFactory factory = new CommonFactory(money ? Material.RECORD_10 : factoryItem.getMaterial(), FactoryType.COMMON, e.getPlayer().getUniqueId(), new HashSet<>(), Factory.COMMON_FACTORY_MAX_HEALTH);
                    ;
                    // Right know all the factory types have the same Factory#build method, TODO: split them

                    e.getPlayer().setItemInHand(null);
                    e.getPlayer().updateInventory();

                    factory.build(target.getRelative(BlockFace.UP), true, plugin);
                    e.getPlayer().teleport(factory.getCenter());
                } else StringUtils.e("You cannot place a factory here", e.getPlayer());
            } else if (FactoryType.isValid(e.getItem())){
                StringUtils.e("You cannot place a factory here", e.getPlayer());
            } else if (e.getClickedBlock() != null) {

                Player player = e.getPlayer();
                Factory factory = Factory.fromLocation(e.getClickedBlock().getLocation());
                if (factory != null) {
                    if (factory.getOwner().equals(player.getUniqueId()) || factory.getAdmins().contains(player.getUniqueId())) {
                        e.getPlayer().openInventory(factory.getInventory());
                    } else StringUtils.e("Your not allowed to access this factory", player);
                }

            }

        }
    }

    @EventHandler
    public void onBreak(BlockBreakEvent e) {

        Factory factory = Factory.fromLocation(e.getBlock().getLocation());

        if (factory != null) {
            e.setCancelled(true);
            if (!factory.isAllowed(e.getPlayer().getUniqueId())) {


                FPlayer fPlayer = FPlayers.getInstance().getByOfflinePlayer(Bukkit.getOfflinePlayer(factory.getOwner()));
                FPlayer breaker = FPlayers.getInstance().getByPlayer(e.getPlayer());

                if (Integer.parseInt(breaker.getFactionId()) > 0) {
                    if (!fPlayer.getFactionId().equalsIgnoreCase(breaker.getFactionId())) {
                        if (factory.getHealth() == 0) {
                            ItemStack bluePrint = factory.generateBluePrint();
                            factory.destroy();
                            if (e.getPlayer().getInventory().firstEmpty() == -1) {
                                e.getPlayer().getWorld().dropItemNaturally(e.getPlayer().getLocation(), bluePrint);
                                StringUtils.e("Blueprint was dropped on ground due to full inventory", e.getPlayer());
                            } else {
                                StringUtils.i("<e>Factory destroyed", e.getPlayer());
                                e.getPlayer().getInventory().addItem(bluePrint);
                            }
                        }
                    }

                }
            }

        }

    }

    @EventHandler
    public void onMove(PlayerMoveEvent e) {

        Factory factory = Factory.fromLocation(e.getTo());

        if (factory != null && factory.getHealth() > 0 && !factory.isAllowed(e.getPlayer().getUniqueId())) {

            Location to = e.getTo();
            Location from = e.getFrom();

            Vector direction = new Vector(from.getX() - to.getX(), from.getY() - to.getY(), from.getZ() - to.getZ()).multiply(10);

            if (e.getPlayer().getLocation().getBlock().getRelative(BlockFace.DOWN).getType().equals(Material.AIR)) {
                e.getPlayer().setVelocity(direction.normalize());
            } else {
                e.getPlayer().setVelocity(direction);
            }

            StringUtils.e("Factory is maintained! Force field active!", e.getPlayer());

        }

    }

}
