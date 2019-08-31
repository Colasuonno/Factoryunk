package com.unknown.factoryunk.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.defaults.BukkitCommand;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public abstract class FineCommand extends BukkitCommand {

    private CommandManager.CommandType type;

    /**
     * Auto-Register commands throw FineCommand constructor
     * @param name of the command
     * @param permission to execute this command
     * @param type who can execute this command
     * @param aliases of the command
     */
    public FineCommand(String name, String permission, CommandManager.CommandType type, List<String> aliases) {
        super(name, "", "", aliases);
        super.setPermission(permission);
        this.type = type;
        Bukkit.getCommandMap().register(name, this);
    }

    public FineCommand(String name, String permission, CommandManager.CommandType type) {
        this(name, permission, type, new ArrayList<>());
    }

    public FineCommand(String name, String permission) {
        this(name, permission, CommandManager.CommandType.ALL);
    }

    public FineCommand(String name) {
        this(name, null);
    }

    /**
     * Command Handler for each FineCommand
     * @param commandSender who perform the command
     * @param s label
     * @param strings command arguments
     * @return true if the command was successful
     */
    @Override
    public boolean execute(CommandSender commandSender, String s, String[] strings) {

        if ((type.equals(CommandManager.CommandType.CONSOLE) && commandSender instanceof Player) || (type.equals(CommandManager.CommandType.PLAYER) && commandSender instanceof ConsoleCommandSender))
            commandSender.sendMessage(ChatColor.RED + "You cannot execute this command");
        else {
            if (isAllowed(commandSender)) {
                run(commandSender, s, strings);
            } else commandSender.sendMessage(ChatColor.RED + "You do not have permission");
        }
        return true;
    }


    /**
     * General run for each fine command, CALLED only if authorized
     * @param sender who perform the command
     * @param label command name
     * @param args command arguments
     */
    public abstract void run(CommandSender sender, String label, String[] args);

    private boolean isAllowed(CommandSender commandSender) {
        String perm = this.getPermission();
        return perm == null || commandSender.hasPermission(perm);
    }

    public CommandManager.CommandType getType() {
        return type;
    }
}
