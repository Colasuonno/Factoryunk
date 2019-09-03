package com.unknown.factoryunk.listener;

import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.FPlayers;
import com.unknown.factoryunk.FactoryUnk;
import com.unknown.factoryunk.items.blueprint.FactoryType;
import com.unknown.factoryunk.items.factories.Factory;
import com.unknown.factoryunk.items.factories.FactoryItem;
import com.unknown.factoryunk.items.factories.types.CommonFactory;
import com.unknown.factoryunk.items.factories.types.LegendaryFactory;
import com.unknown.factoryunk.utils.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.Plugin;

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


                Block target = e.getPlayer().getTargetBlock((HashSet<Byte>) null, 10);
                if (target == null) {
                    StringUtils.e("Location not valid", e.getPlayer());
                    return;
                }

                if (plugin.getFactories().tooClose(target.getLocation())) {
                    StringUtils.e("You are too close to a factory", e.getPlayer());
                    return;
                }

                FactoryItem factoryItem = new FactoryItem(e.getItem());

                Factory factory;
                // Right know all the factory types have the same Factory#build method, TODO: split them
                switch (factoryItem.getType()) {
                    case LEGENDARY:
                        factory = new LegendaryFactory(factoryItem.getMaterial(), factoryItem.getType(), e.getPlayer().getUniqueId(), new HashSet<>(), Factory.FACTORY_MAX_HEALTH);
                        break;
                    case COMMON:
                        factory = new CommonFactory(factoryItem.getMaterial(), factoryItem.getType(), e.getPlayer().getUniqueId(), new HashSet<>(), Factory.FACTORY_MAX_HEALTH);
                        break;
                    default:
                        factory = new Factory(factoryItem.getMaterial(), factoryItem.getType(), e.getPlayer().getUniqueId(), new HashSet<>(), Factory.FACTORY_MAX_HEALTH);
                        break;
                }
                factory.build(target.getRelative(BlockFace.UP), true, plugin);
            } else if (e.getClickedBlock() != null) {

                Player player = e.getPlayer();
                Factory factory = Factory.fromLocation(e.getClickedBlock().getLocation());
                if (factory != null) {
                    if (factory.getOwner().equals(player.getUniqueId()) || factory.getAdmins().contains(player.getUniqueId())){
                        e.getPlayer().openInventory(factory.getInventory());
                    } else StringUtils.e("Your not allowed to access this factory", player);
                }

            }

        }
    }

}
