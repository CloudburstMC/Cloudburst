package org.cloudburstmc.server.blockentity.impl;

import com.nukkitx.math.vector.Vector3i;
import org.cloudburstmc.server.blockentity.BlockEntityType;
import org.cloudburstmc.server.level.chunk.Chunk;

public class UnknownBlockEntity extends BaseBlockEntity {

    public UnknownBlockEntity(BlockEntityType<?> type, Chunk chunk, Vector3i position) {
        super(type, chunk, position);
    }

    @Override
    public boolean isValid() {
        return true;
    }
}
