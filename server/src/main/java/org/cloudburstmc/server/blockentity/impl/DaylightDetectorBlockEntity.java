package org.cloudburstmc.server.blockentity.impl;

import com.nukkitx.math.vector.Vector3i;
import org.cloudburstmc.server.block.BlockType;
import org.cloudburstmc.server.block.BlockTypes;
import org.cloudburstmc.server.blockentity.BlockEntityType;
import org.cloudburstmc.server.blockentity.DaylightDetector;
import org.cloudburstmc.server.level.chunk.Chunk;

public class DaylightDetectorBlockEntity extends BaseBlockEntity implements DaylightDetector {


    public DaylightDetectorBlockEntity(BlockEntityType<?> type, Chunk chunk, Vector3i position) {
        super(type, chunk, position);
    }

    @Override
    public boolean isValid() {
        BlockType blockId = getBlockState().getType();
        return blockId == BlockTypes.DAYLIGHT_DETECTOR || blockId == BlockTypes.DAYLIGHT_DETECTOR_INVERTED;
    }

}
