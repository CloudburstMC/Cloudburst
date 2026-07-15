package org.cloudburstmc.server.blockentity;

import org.cloudburstmc.api.block.BlockType;
import org.cloudburstmc.api.blockentity.BlockEntityType;
import org.cloudburstmc.api.blockentity.Smoker;
import org.cloudburstmc.api.level.chunk.Chunk;
import org.cloudburstmc.math.vector.Vector3i;

import static org.cloudburstmc.api.block.BlockTypes.LIT_SMOKER;
import static org.cloudburstmc.api.block.BlockTypes.SMOKER;

/**
 * Block entity implementation for a smoker. Smelts food items at twice the speed of a regular furnace
 * and only accepts {@code SMOKER} / {@code LIT_SMOKER} block types.
 */
public class SmokerBlockEntity extends FurnaceBlockEntity implements Smoker {

    public SmokerBlockEntity(BlockEntityType<?> type, Chunk chunk, Vector3i position) {
        super(type, chunk, position);
    }

    @Override
    public float getBurnRate() {
        return 2.0f;
    }

    @Override
    protected BlockType getLitType() {
        return LIT_SMOKER;
    }

    @Override
    protected BlockType getUnlitType() {
        return SMOKER;
    }
}
