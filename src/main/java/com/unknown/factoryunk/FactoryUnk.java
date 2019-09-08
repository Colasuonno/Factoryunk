package com.unknown.factoryunk;

import com.unknown.factoryunk.commands.CommandManager;
import com.unknown.factoryunk.gui.listeners.InventoryListener;
import com.unknown.factoryunk.items.factories.Factories;
import com.unknown.factoryunk.items.factories.Factory;
import com.unknown.factoryunk.listener.PlayerListener;
import com.unknown.factoryunk.task.FactoryChecker;
import com.unknown.factoryunk.utils.StringUtils;
import com.unknown.factoryunk.utils.YamlConfig;
import lombok.Getter;
import net.milkbowl.vault.economy.Economy;
import net.minecraft.server.v1_8_R3.TileEntity;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class FactoryUnk extends JavaPlugin {

    @Getter
    private CommandManager commandManager;
    @Getter
    private Factories factories;
    @Getter
    private Economy economy;

    @Override
    public void onEnable() {

        // Check for Vault<->Economy dependency
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) StringUtils.e("No Economy found! Money Factory will not work", Bukkit.getConsoleSender());
        else economy = rsp.getProvider();

        // Assuming we want to remove all of them, load: POSTWORLD
        for (World worlds : Bukkit.getWorlds()) {
            for (Entity tileEntity : worlds.getEntities()) {
                if (tileEntity instanceof ArmorStand) {
                    tileEntity.remove();
                }
            }
        }

        YamlConfig.create(this, "factories", true);

        new InventoryListener(this);
        new PlayerListener(this);

        new FactoryChecker().runTaskTimer(this, 0L, 10L);

        factories = new Factories(this);
        commandManager = new CommandManager(this);

        commandManager.enable();
        factories.load(true);
    }

    @Override
    public void onDisable() {
        for (Factory factory : Factory.getFactories()) {
            if (factory.getLines() != null) {
                factory.getLines().destroy();
            }
            factory.save(this);
        }
    }
}
