package org.cloudburstmc.api.event.level;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.event.Cancellable;
import org.cloudburstmc.api.event.Event;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.math.vector.Vector3f;

import java.util.List;
import java.util.Objects;

/**
 * Fired after an explosion damages entities but before it changes blocks.
 * Cancelling this event prevents block destruction and explosion fire without cancelling entity damage.
 */
public class ExplosionEvent extends Event implements Cancellable {

    private final Level level;
    private final Vector3f position;
    private final @Nullable Entity sourceEntity;
    private final @Nullable Block sourceBlock;
    private final List<Block> blocks;
    private float yield;

    public ExplosionEvent(Level level, Vector3f position, @Nullable Entity sourceEntity, @Nullable Block sourceBlock, List<Block> blocks, float yield) {
        this.level = Objects.requireNonNull(level, "level");
        this.position = Objects.requireNonNull(position, "position");
        this.sourceEntity = sourceEntity;
        this.sourceBlock = sourceBlock;
        this.blocks = Objects.requireNonNull(blocks, "blocks");
        this.setYield(yield);
    }

    /**
     * Returns the level where the explosion occurs.
     */
    public Level getLevel() {
        return this.level;
    }

    /**
     * Returns the explosion center.
     */
    public Vector3f getPosition() {
        return this.position;
    }

    /**
     * Returns the direct source entity, or {@code null} for an environmental explosion.
     */
    public @Nullable Entity getSourceEntity() {
        return this.sourceEntity;
    }

    /**
     * Returns the source block, or {@code null} for an entity or environmental explosion.
     */
    public @Nullable Block getSourceBlock() {
        return this.sourceBlock;
    }

    /**
     * Returns the mutable list of blocks to destroy when block interaction is enabled.
     */
    public List<Block> getBlocks() {
        return this.blocks;
    }

    /**
     * Returns the probability of a block's loot surviving the explosion.
     */
    public float getYield() {
        return this.yield;
    }

    /**
     * Sets the probability that each dropped item survives the blast.
     */
    public void setYield(float yield) {
        if (!Float.isFinite(yield) || yield < 0 || yield > 1) {
            throw new IllegalArgumentException("Explosion yield must be between 0 and 1");
        }

        this.yield = yield;
    }
}
