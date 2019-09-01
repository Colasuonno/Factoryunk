package com.unknown.factoryunk.items.factories.types;

import com.unknown.factoryunk.items.blueprint.FactoryType;
import com.unknown.factoryunk.items.factories.Factory;
import org.bukkit.Location;
import org.bukkit.Material;

import java.util.Set;
import java.util.UUID;

public class CommonFactory extends Factory {

    public CommonFactory(UUID owner, Location blockLocation, Set<UUID> admins, Material material, int health, FactoryType type, Location center, Location pos1, Location pos2) {
        super(owner, blockLocation, admins, material, health, type, center, pos1, pos2);
    }

    public CommonFactory(Material material, FactoryType type, UUID owner, Set<UUID> admins, int health) {
        super(material, type, owner, admins, health);
    }

    @Override
    public void reward(){
        setLastDrop(System.currentTimeMillis());
        System.out.println("EHEHEHE");
    }

}


