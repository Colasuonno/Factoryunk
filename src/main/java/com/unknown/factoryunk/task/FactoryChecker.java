package com.unknown.factoryunk.task;

import com.unknown.factoryunk.items.blueprint.FactoryType;
import com.unknown.factoryunk.items.factories.Factory;
import com.unknown.factoryunk.items.factories.types.CommonFactory;
import com.unknown.factoryunk.items.factories.types.LegendaryFactory;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Sync-> Task for factory reward
 * (Not chosen) Async-> This task can run async (not-safe)
 */
public class FactoryChecker extends BukkitRunnable {

    @Override
    public void run() {

        Factory.getFactories()
                .stream()
                .filter(factory -> factory.getOwner() != null)
                .filter(Factory::canDrop)
                .forEach(Factory::reward);

    }
}
