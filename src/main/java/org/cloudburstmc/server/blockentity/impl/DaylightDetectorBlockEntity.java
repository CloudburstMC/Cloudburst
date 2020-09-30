package org.cloudburstmc.server.blockentity.impl;

import com.nukkitx.math.vector.Vector3i;
import org.cloudburstmc.server.block.BlockIds;
import org.cloudburstmc.server.blockentity.BlockEntityType;
import org.cloudburstmc.server.blockentity.DaylightDetector;
import org.cloudburstmc.server.level.chunk.Chunk;
import org.cloudburstmc.server.utils.Identifier;

public class DaylightDetectorBlockEntity extends BaseBlockEntity implements DaylightDetector {


    public DaylightDetectorBlockEntity(BlockEntityType<?> type, Chunk chunk, Vector3i position) {
        super(type, chunk, position);
    }

    @Override
    public boolean isValid() {
        Identifier blockId = getBlockState().getType();
        return blockId == BlockIds.DAYLIGHT_DETECTOR || blockId == BlockIds.DAYLIGHT_DETECTOR_INVERTED;
    }

}
