package com.unknown.factoryunk.commands.execute;

import com.google.common.collect.Lists;
import com.unknown.factoryunk.commands.CommandManager;
import com.unknown.factoryunk.commands.FineCommand;
import com.unknown.factoryunk.items.blueprint.FactoryType;
import com.unknown.factoryunk.items.factories.Factory;
import com.unknown.factoryunk.items.factories.FactoryItem;
import com.unknown.factoryunk.utils.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class BluePrintCommand extends FineCommand {

    public BluePrintCommand() {
        super("blueprint", "factoryunk.blueprint", CommandManager.CommandType.ALL, Lists.newArrayList("bp"));
    }

    @Override
    public void run(CommandSender sender, String label, String[] args) {
        if (args.length == 3 && args[0].equalsIgnoreCase("give")){

            Player playerSender = (Player) sender; // since is a CommandType#PLAYER FineCommand
            Player player = Bukkit.getPlayer(args[1]);
            if (player == null) StringUtils.e("Player not found", sender);
            else {

                Material material = null;
                boolean money = false;

                if (args[2].equalsIgnoreCase("money")){
                    money = true;
                } else {

                    try {
                        material = Material.valueOf(args[2].toUpperCase());
                    } catch (IllegalArgumentException e) {
                        StringUtils.e("Material not found", sender);
                        return;
                    }


                }
                if (!money && !FactoryType.isMaterialValid(material)){
                    StringUtils.e(material + " is not a valid material", sender);
                    return;
                }

                FactoryItem factoryItem = null;
                if (!money) factoryItem = new FactoryItem(material);
                playerSender.getInventory().addItem(new Factory(money ? Material.RECORD_10 : factoryItem.getMaterial(), money ? FactoryType.COMMON : factoryItem.getType()).generateBluePrint());
            }

        } else StringUtils.e("/blueprint give <player> <material>", sender);

    }
}
