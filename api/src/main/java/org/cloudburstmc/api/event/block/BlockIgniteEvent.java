package org.cloudburstmc.api.event.block;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.event.Cancellable;

import java.util.Objects;

/**
 * Fired before fire is placed or an existing block is lit. Cancelling prevents the ignition.
 */
public class BlockIgniteEvent extends BlockEvent implements Cancellable {

    private final @Nullable Block ignitingBlock;
    private final @Nullable Entity ignitingEntity;
    private final BlockIgniteCause cause;

    /**
     * @param block          the block being lit or replaced by fire
     * @param cause          the ignition cause
     * @param ignitingEntity the responsible entity, or {@code null}
     * @param ignitingBlock  the responsible block, or {@code null}
     */
    public BlockIgniteEvent(Block block, BlockIgniteCause cause, @Nullable Entity ignitingEntity, @Nullable Block ignitingBlock) {
        super(block);
        this.cause = Objects.requireNonNull(cause, "cause");
        this.ignitingEntity = ignitingEntity;
        this.ignitingBlock = ignitingBlock;
    }

    /**
     * Returns the responsible block, or {@code null} if there was none.
     */
    public @Nullable Block getIgnitingBlock() {
        return this.ignitingBlock;
    }

    /**
     * Returns the responsible entity, or {@code null} if there was none.
     */
    public @Nullable Entity getIgnitingEntity() {
        return this.ignitingEntity;
    }

    public BlockIgniteCause getCause() {
        return this.cause;
    }
}
