package org.cloudburstmc.api.blockentity;

import org.cloudburstmc.api.util.Identifier;

import java.util.concurrent.ConcurrentHashMap;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Canonical identifier for a family of block entities sharing one public API type.
 * Use {@link org.cloudburstmc.api.registry.BlockEntityRegistry} to register its
 * factory and valid block types.
 *
 * @param <T> public block entity API type
 */
public final class BlockEntityType<T extends BlockEntity> {

    private static final ConcurrentHashMap<Identifier, BlockEntityType<?>> TYPES = new ConcurrentHashMap<>();

    private final Identifier identifier;
    private final Class<T> blockEntityClass;

    private BlockEntityType(Identifier identifier, Class<T> blockEntityClass) {
        this.identifier = identifier;
        this.blockEntityClass = blockEntityClass;
    }

    /**
     * Returns the canonical type for an identifier and API class.
     *
     * @param identifier type identifier
     * @param entityClass public API class represented by the token
     * @return canonical type token
     * @throws IllegalArgumentException if the identifier is already associated with another class
     */
    public static <T extends BlockEntity> BlockEntityType<T> from(String identifier, Class<T> entityClass) {
        return from(Identifier.parse(identifier), entityClass);
    }

    /**
     * Returns the canonical type for an identifier and API class.
     *
     * @param identifier type identifier
     * @param entityClass public API class represented by the token
     * @return canonical type token
     * @throws IllegalArgumentException if the identifier is already associated with another class
     */
    public static <T extends BlockEntity> BlockEntityType<T> from(Identifier identifier, Class<T> entityClass) {
        checkNotNull(identifier, "identifier");
        checkNotNull(entityClass, "entityClass");
        checkArgument(BlockEntity.class.isAssignableFrom(entityClass), "%s is not subclass of BlockEntity", entityClass.getName());
        BlockEntityType<?> type = TYPES.compute(identifier, (ignored, existing) -> {
            if (existing == null) {
                return new BlockEntityType<>(identifier, entityClass);
            }
            checkArgument(existing.blockEntityClass == entityClass,
                    "Block entity type %s already represents %s, not %s",
                    identifier, existing.blockEntityClass.getName(), entityClass.getName());
            return existing;
        });
        @SuppressWarnings("unchecked")
        BlockEntityType<T> typed = (BlockEntityType<T>) type;
        return typed;
    }

    public Identifier getIdentifier() {
        return identifier;
    }

    public Class<T> getBlockEntityClass() {
        return blockEntityClass;
    }

    @Override
    public String toString() {
        return identifier + "(" + blockEntityClass + ")";
    }
}
