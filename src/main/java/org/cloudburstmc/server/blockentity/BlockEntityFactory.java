package org.cloudburstmc.server.blockentity;

import com.nukkitx.math.vector.Vector3i;
import org.cloudburstmc.server.world.chunk.Chunk;

public interface BlockEntityFactory<T extends BlockEntity> {

    T create(BlockEntityType<T> type, Chunk chunk, Vector3i position);
}
