package org.cloudburstmc.api.registry;

import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockType;
import org.cloudburstmc.api.blockentity.BlockEntity;
import org.cloudburstmc.api.blockentity.BlockEntityFactory;
import org.cloudburstmc.api.blockentity.BlockEntityType;
import org.cloudburstmc.api.event.server.ServerInitializationEvent;
import org.cloudburstmc.api.util.Identifier;

import java.util.Set;

/**
 * Registers block-entity types and determines which block states may host them.
 */
public interface BlockEntityRegistry extends KeyedRegistry<BlockEntityType<?>> {

    @Override
    default Identifier getId(BlockEntityType<?> value) {
        return value.getId();
    }

    /**
     * Tests the registered valid-block association for a type.
     *
     * @param type  block-entity type
     * @param state prospective host state
     * @return whether {@code state} may host {@code type}
     */
    boolean isValid(BlockEntityType<?> type, BlockState state);

    /**
     * Registers a custom block-entity type. Plugins should register types while handling
     * {@link ServerInitializationEvent}. Calls made after registries close are rejected.
     *
     * @param type         custom, non-{@code minecraft} namespaced type key
     * @param factory      factory used to create instances
     * @param persistentId unique persistent identifier
     * @param validBlocks  non-empty set of block types that may host the entity
     * @throws RegistryException if registration is closed, the type is already registered,
     *                           or the persistent identifier is already in use
     * @see ServerInitializationEvent
     */
    <T extends BlockEntity> void register(BlockEntityType<T> type, BlockEntityFactory<T> factory,
                                          String persistentId, Set<BlockType> validBlocks) throws RegistryException;
}
