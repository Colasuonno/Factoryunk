package com.unknown.factoryunk.task;

import com.unknown.factoryunk.items.blueprint.FactoryType;
import com.unknown.factoryunk.items.factories.Factory;
import com.unknown.factoryunk.items.factories.types.CommonFactory;
import com.unknown.factoryunk.items.factories.types.LegendaryFactory;
import com.unknown.factoryunk.items.factories.workers.FactoryWorker;
import org.bukkit.Location;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Sync-> Task for factory reward
 * (Not chosen) Async-> This task can run async (not-safe)
 */
public class FactoryChecker extends BukkitRunnable {

    @Override
    public void run() {

        /*
           Select all valid factory, check they can collect items, than reward
         */
        Factory.getFactories()
                .stream()
                .filter(factory -> factory.getOwner() != null)
                .filter(Factory::canDrop)
                .forEach(Factory::reward);

        /*
           Select all valid factory with at least 1 factory worker, than check if he's able to collect, than reward
         */
        Factory.getFactories()
                .stream()
                .filter(factory -> factory.getOwner() != null && factory.getWorkers().size() > 0 && factory.getHealth() > 0)
                .forEach(factory ->
                        factory.getWorkers()
                        .stream()
                        .filter(FactoryWorker::canDrop)
                        .forEach(FactoryWorker::reward)
                );


        /*
           Modify the alpha value of all factory workers
         */
        Factory.getFactories()
                .stream()
                .filter(factory -> !factory.getWorkers().isEmpty())
                .forEach(factory -> {

                    if (factory.getHealth() <= 20){
                        factory.repair(20);
                    }

                    Location center = factory.getCenter();

                    factory.getWorkers()
                            .stream()
                            .filter(worker -> worker.getNPC() != null && worker.getNPC().isSpawned() && worker.getNPC().isProtected())
                            .forEach(worker -> {

                                /*
                                  Circle geometry 2D
                                 */

                                double alpha = worker.getAlpha();

                                double x = Math.cos(alpha) * 3;
                                double z = Math.sin(alpha) * 3; // y

                                worker.getNPC().getNavigator().setTarget(center.clone().add(x, 0.0D, z));

                                worker.setAlpha(worker.getAlpha() + 1.0D);
                            });
                });

    }
}
