package com.aibuilder.model;

import lombok.Data;

/**
 * Represents a single block placement instruction
 */
@Data
public class BuildInstruction {
    private int x;
    private int y;
    private int z;
    private String material;
    private String data; // For block data like stairs direction, etc.

    public BuildInstruction() {}

    public BuildInstruction(int x, int y, int z, String material) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.material = material;
    }

    public BuildInstruction(int x, int y, int z, String material, String data) {
        this(x, y, z, material);
        this.data = data;
    }
}
