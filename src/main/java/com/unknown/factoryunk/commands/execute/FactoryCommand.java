package com.unknown.factoryunk.commands.execute;

import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.FPlayers;
import com.unknown.factoryunk.commands.CommandManager;
import com.unknown.factoryunk.commands.FineCommand;
import com.unknown.factoryunk.items.factories.Factories;
import com.unknown.factoryunk.items.factories.Factory;
import com.unknown.factoryunk.utils.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class FactoryCommand extends FineCommand {

    public FactoryCommand() {
        super("factory", "factoryunk.factory", CommandManager.CommandType.PLAYER);

    }

    // USAGE /factory admin <player>

    @Override
    public void run(CommandSender sender, String label, String[] args) {

        Player commandSender = (Player) sender;


        if (args.length == 2 && args[0].equalsIgnoreCase("admin")) {

            Player player = Bukkit.getPlayer(args[1]);
            if (player != null) {
                Factory factory = Factory.fromLocation(commandSender.getLocation());
                if (factory != null) {
                    if (factory.getOwner().equals(commandSender.getUniqueId())) {
                        if (factory.getOwner().equals(player.getUniqueId())) {
                            StringUtils.e("You cannot add yourself", commandSender);
                        } else {
                            if (factory.getAdmins().contains(player.getUniqueId())) {
                                StringUtils.e("Admin already added", commandSender);
                            } else {

                                FPlayer fsender = FPlayers.getInstance().getByPlayer(commandSender);
                                FPlayer target = FPlayers.getInstance().getByOfflinePlayer(player);

                                if (fsender.getFactionId().equalsIgnoreCase(target.getFactionId())) {
                                    factory.addAdmin(player.getUniqueId());
                                    StringUtils.i(player.getName() + " has been added as admin", commandSender);
                                } else StringUtils.e("You cannot add " + target.getName(), commandSender);
                            }
                        }
                    } else StringUtils.e("Your not the owner of the factory", commandSender);
                } else StringUtils.e("No factory found! Make sure to be in the factory", commandSender);
            } else StringUtils.e("Player not found", sender);

        } else if (args.length == 1 && args[0].equalsIgnoreCase("menu")) {
            commandSender.openInventory(Factories.getFactories(commandSender.getUniqueId()));
        } else StringUtils.e("/factory (admin|menu) (<player>|<none>)", sender);
    }
}
