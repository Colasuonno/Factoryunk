package com.unknown.factoryunk.task;

import com.unknown.factoryunk.items.blueprint.FactoryType;
import com.unknown.factoryunk.items.factories.Factory;
import com.unknown.factoryunk.items.factories.types.CommonFactory;
import com.unknown.factoryunk.items.factories.types.LegendaryFactory;
import org.bukkit.Location;
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

        Factory.getFactories()
                .stream()
                .filter(factory -> !factory.getWorkers().isEmpty())
                .forEach(factory -> {

                    Location center = factory.getCenter();

                    factory.getWorkers()
                            .stream()
                            .filter(worker -> worker.getNPC() != null && worker.getNPC().isSpawned() && worker.getNPC().isProtected())
                            .forEach(worker -> {
                                double alpha = worker.getAlpha();

                                double x = Math.cos(alpha) * 3;
                                double z = Math.sin(alpha) * 3; // y

                                worker.getNPC().getNavigator().setTarget(center.clone().add(x, 0.0D, z));

                                worker.setAlpha(worker.getAlpha() + 1.0D);
                            });
                });

    }
}
