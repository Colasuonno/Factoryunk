package com.unknown.factoryunk.items.factories.workers;

import com.unknown.factoryunk.items.factories.Factory;
import lombok.Data;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;

@Data public class FactoryWorker {

    private int id;
    private double alpha;
    private int collectedItems;
    private long delayInMillis = 3000L;
    private long lastDrop;

    // Constant
    public static final int STORAGE_AMOUNT = 64 * 5; // 5 stacks

    public FactoryWorker(int id, int collectedItems) {
        this.id = id;
        this.alpha = 0.0D;
        this.collectedItems = collectedItems;
    }

    /**
     * Gets the Citizen NPC instance
     * @return the npc
     */
    public NPC getNPC(){
        return CitizensAPI.getNPCRegistry().getById(id);
    }

    /**
     * Add an item to collectedItems (BYPASSING limits)
     * @param amount
     */
    public void collect(int amount){
        this.collectedItems += amount;
    }

    /**
     * Reward function for each delay
     */
    public void reward(){
        setLastDrop(System.currentTimeMillis());
        collect(1);
    }


    /**
     * Checks if the NPC is ready to reward
     * @return if can reward
     */
    public boolean canDrop(){
        return this.collectedItems < STORAGE_AMOUNT && System.currentTimeMillis() - lastDrop >= delayInMillis;
    }


}

