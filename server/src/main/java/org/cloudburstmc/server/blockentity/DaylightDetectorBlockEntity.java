package org.cloudburstmc.server.blockentity;

import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.blockentity.BlockEntityType;
import org.cloudburstmc.api.blockentity.DaylightDetector;
import org.cloudburstmc.api.level.chunk.Chunk;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.block.component.DaylightDetectorBlockHandlers;

public class DaylightDetectorBlockEntity extends BaseBlockEntity implements DaylightDetector {

    public DaylightDetectorBlockEntity(BlockEntityType<?> type, Chunk chunk, Vector3i position) {
        super(type, chunk, position);
    }

    @Override
    public boolean isInverted() {
        return this.getBlockState().getType() == BlockTypes.DAYLIGHT_DETECTOR_INVERTED;
    }

    @Override
    public void setInverted(boolean inverted) {
        if (!this.isClosed()) {
            DaylightDetectorBlockHandlers.setInverted(this.getBlock(), inverted);
        }
    }
}
