package org.cloudburstmc.server.registry;

import org.cloudburstmc.api.block.BlockType;

public record VanillaSlabAndStairFamily(BlockType base, BlockType slab, BlockType doubleSlab, BlockType stairs) {
}
