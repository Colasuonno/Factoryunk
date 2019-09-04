package com.unknown.factoryunk.items.factories.workers;

import com.unknown.factoryunk.items.factories.Factory;
import lombok.Data;

@Data public class FactoryWorker {

    private int id;
    private Factory factory;

    public FactoryWorker(int id, Factory factory) {
        this.id = id;
        this.factory = factory;
    }

    public void check(){

    }

}

