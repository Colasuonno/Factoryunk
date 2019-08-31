package com.unknown.factoryunk.items.factories;

import lombok.Data;
import lombok.ToString;

@Data @ToString
public class Schematic {

    private String name;
    private short[] blocks;
    private byte[] data;
    private short width;
    private short length;
    private short height;

    public Schematic(String name, short[] blocks, byte[] data, short width, short length, short height) {
        this.name = name;
        this.blocks = blocks;
        this.data = data;
        this.width = width;
        this.length = length;
        this.height = height;
    }

}
