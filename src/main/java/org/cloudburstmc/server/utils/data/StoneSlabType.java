package org.cloudburstmc.server.utils.data;

import lombok.RequiredArgsConstructor;
import org.cloudburstmc.server.utils.Identifier;

import static org.cloudburstmc.server.block.BlockIds.*;

@RequiredArgsConstructor
public enum StoneSlabType {
    SMOOTH_STONE(STONE_SLAB),
    SANDSTONE(STONE_SLAB),
    WOOD(STONE_SLAB),
    COBBLESTONE(STONE_SLAB),
    BRICK(STONE_SLAB),
    STONE_BRICK(STONE_SLAB),
    QUARTZ(STONE_SLAB),
    NETHER_BRICK(STONE_SLAB),
    RED_SANDSTONE(STONE_SLAB2),
    PURPUR(STONE_SLAB2),
    PRISMARINE_ROUGH(STONE_SLAB2),
    PRISMARINE_DARK(STONE_SLAB2),
    PRISMARINE_BRICK(STONE_SLAB2),
    MOSSY_COBBLESTONE(STONE_SLAB2),
    SMOOTH_SANDSTONE(STONE_SLAB2),
    RED_NETHER_BRICK(STONE_SLAB2),
    END_STONE_BRICK(STONE_SLAB3),
    SMOOTH_RED_SANDSTONE(STONE_SLAB3),
    POLISHED_ANDESITE(STONE_SLAB3),
    ANDESITE(STONE_SLAB3),
    DIORITE(STONE_SLAB3),
    POLISHED_DIORITE(STONE_SLAB3),
    GRANITE(STONE_SLAB3),
    POLISHED_GRANITE(STONE_SLAB3),
    MOSSY_STONE_BRICK(STONE_SLAB4),
    SMOOTH_QUARTZ(STONE_SLAB4),
    STONE(STONE_SLAB4),
    CUT_SANDSTONE(STONE_SLAB4),
    CUT_RED_SANDSTONE(STONE_SLAB4);

    private final Identifier identifier;

    public Identifier getIdentifier() {
        return identifier;
    }
}
