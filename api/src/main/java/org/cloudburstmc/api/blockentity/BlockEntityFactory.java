package org.cloudburstmc.api.blockentity;

import com.nukkitx.math.vector.Vector3i;
import org.cloudburstmc.api.level.chunk.Chunk;

public interface BlockEntityFactory<T extends BlockEntity> {

    T create(BlockEntityType<T> type, Chunk chunk, Vector3i position);
}
