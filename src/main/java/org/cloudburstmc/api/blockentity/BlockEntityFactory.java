package org.cloudburstmc.api.blockentity;

import org.cloudburstmc.api.level.chunk.Chunk;
import org.cloudburstmc.math.vector.Vector3i;

public interface BlockEntityFactory<T extends BlockEntity> {

    T create(BlockEntityType<T> type, Chunk chunk, Vector3i position);
}
