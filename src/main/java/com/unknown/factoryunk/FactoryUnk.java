package com.unknown.factoryunk;

import com.unknown.factoryunk.commands.CommandManager;
import com.unknown.factoryunk.gui.listeners.InventoryListener;
import com.unknown.factoryunk.items.factories.Factories;
import com.unknown.factoryunk.items.factories.Factory;
import com.unknown.factoryunk.listener.PlayerListener;
import com.unknown.factoryunk.task.FactoryChecker;
import com.unknown.factoryunk.utils.YamlConfig;
import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;

public class FactoryUnk extends JavaPlugin {

    @Getter private CommandManager commandManager;
    @Getter private Factories factories;

    @Override
    public void onEnable() {

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
        for (Factory factory : Factory.getFactories()){
            if (factory.getLines() != null){
                factory.getLines().destroy();
            }
        }
    }
}
