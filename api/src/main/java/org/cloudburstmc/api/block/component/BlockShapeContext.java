package org.cloudburstmc.api.block.component;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.math.vector.Vector3i;

import static java.util.Objects.requireNonNull;

/**
 * Level and position for a shape calculation, or an empty context for state-only queries.
 */
public class BlockShapeContext {

    private static final BlockShapeContext EMPTY = new BlockShapeContext(null, null);

    private final @Nullable Level level;
    private final @Nullable Vector3i position;

    private BlockShapeContext(@Nullable Level level, @Nullable Vector3i position) {
        this.level = level;
        this.position = position;
    }

    /**
     * @return the shared context used when no level lookup is permitted
     */
    public static BlockShapeContext empty() {
        return EMPTY;
    }

    /**
     * Creates a context for a block position without loading or querying it.
     *
     * @param level    owning level
     * @param position block position
     * @return level-aware context
     */
    public static BlockShapeContext at(Level level, Vector3i position) {
        return new BlockShapeContext(requireNonNull(level, "level"), requireNonNull(position, "position"));
    }

    /**
     * @return whether neither a level nor a position is available
     */
    public boolean isEmpty() {
        return this.level == null;
    }

    /**
     * @return owning level
     * @throws IllegalStateException for a context-free query
     */
    public Level level() {
        if (this.level == null) {
            throw new IllegalStateException("Context-free shape query has no level");
        }
        return this.level;
    }

    /**
     * @return block position
     * @throws IllegalStateException for a context-free query
     */
    public Vector3i position() {
        if (this.position == null) {
            throw new IllegalStateException("Context-free shape query has no position");
        }
        return this.position;
    }
}
