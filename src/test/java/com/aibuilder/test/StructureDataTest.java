package com.aibuilder.test;

import com.aibuilder.model.BuildInstruction;
import com.aibuilder.model.StructureData;
import org.junit.Test;
import static org.junit.Assert.*;

import java.util.Arrays;

/**
 * Unit tests for structure data models
 */
public class StructureDataTest {

    @Test
    public void testBuildInstructionCreation() {
        BuildInstruction instruction = new BuildInstruction(1, 2, 3, "STONE");
        
        assertEquals(1, instruction.getX());
        assertEquals(2, instruction.getY());
        assertEquals(3, instruction.getZ());
        assertEquals("STONE", instruction.getMaterial());
    }

    @Test
    public void testBuildInstructionWithData() {
        BuildInstruction instruction = new BuildInstruction(0, 0, 0, "OAK_STAIRS", "facing=north");
        
        assertEquals("OAK_STAIRS", instruction.getMaterial());
        assertEquals("facing=north", instruction.getData());
    }    @Test
    public void testStructureDataCreation() {
        StructureData.Size size = new StructureData.Size();
        size.setWidth(5);
        size.setHeight(3);
        size.setDepth(5);

        StructureData.Block block1 = new StructureData.Block(0, 0, 0, "STONE");
        StructureData.Block block2 = new StructureData.Block(1, 0, 0, "STONE");

        StructureData structure = new StructureData();
        structure.setName("Test Structure");
        structure.setDescription("A simple test structure");
        structure.setSize(size);
        structure.setBlocks(Arrays.asList(block1, block2));

        assertEquals("Test Structure", structure.getName());
        assertEquals("A simple test structure", structure.getDescription());
        assertEquals(5, structure.getSize().getWidth());
        assertEquals(2, structure.getBlocks().size());
    }
}
