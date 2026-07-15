package org.cloudburstmc.server.blockentity;

import org.cloudburstmc.api.block.BlockType;
import org.cloudburstmc.api.blockentity.BlastFurnace;
import org.cloudburstmc.api.blockentity.BlockEntityType;
import org.cloudburstmc.api.level.chunk.Chunk;
import org.cloudburstmc.math.vector.Vector3i;

import static org.cloudburstmc.api.block.BlockTypes.BLAST_FURNACE;
import static org.cloudburstmc.api.block.BlockTypes.LIT_BLAST_FURNACE;

/**
 * Block entity implementation for a blast furnace. Smelts items at twice the speed of a regular furnace.
 */
public class BlastFurnaceBlockEntity extends FurnaceBlockEntity implements BlastFurnace {

    public BlastFurnaceBlockEntity(BlockEntityType<?> type, Chunk chunk, Vector3i position) {
        super(type, chunk, position);
    }

    @Override
    public float getBurnRate() {
        return 2.0f;
    }

    @Override
    protected BlockType getLitType() {
        return LIT_BLAST_FURNACE;
    }

    @Override
    protected BlockType getUnlitType() {
        return BLAST_FURNACE;
    }
}
