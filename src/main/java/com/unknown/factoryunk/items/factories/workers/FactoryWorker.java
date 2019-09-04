package com.unknown.factoryunk.items.factories.workers;

import com.unknown.factoryunk.items.factories.Factory;
import lombok.Data;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;

@Data public class FactoryWorker {

    private int id;
    private double alpha;
    private int storageAmount = 64 * 5; // 5 stacks
    private int collectedItems = 0;

    public FactoryWorker(int id, int collectedItems) {
        this.id = id;
        this.alpha = 0.0D;
        this.collectedItems = collectedItems;
    }

    public NPC getNPC(){
        return CitizensAPI.getNPCRegistry().getById(id);
    }

    public void check(){

    }

}

