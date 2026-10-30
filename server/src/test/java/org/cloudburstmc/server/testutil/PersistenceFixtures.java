package org.cloudburstmc.server.testutil;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.blockentity.BlockEntity;
import org.cloudburstmc.api.blockentity.BlockEntityType;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.level.chunk.CloudChunk;
import org.cloudburstmc.server.registry.CloudBlockEntityRegistry;

/**
 * Builds real persistent objects backed by stub worlds, so round-trip tests
 * exercise the production save and load paths.
 */
@UtilityClass
public class PersistenceFixtures {

    /**
     * Creates a block entity of the given type through the registry factory,
     * backed by a real chunk on a stub level.
     *
     * @param type     the block-entity type
     * @param position the position of the entity
     * @param <T>      the entity class
     * @return a real instance, constructed through the registered factory
     */
    public static <T extends BlockEntity> T blockEntity(BlockEntityType<T> type, Vector3i position) {
        Level level = InterfaceProxy.create(Level.class);
        CloudChunk chunk = new CloudChunk(position.getX() >> 4, position.getZ() >> 4, level);
        return CloudBlockEntityRegistry.get().newEntity(type, chunk, position);
    }
}
