package org.cloudburstmc.api.event.block;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.event.Cancellable;

import java.util.Objects;

/**
 * Fired before a TNT block becomes a primed TNT entity.
 */
public class TntPrimeEvent extends BlockEvent implements Cancellable {

    private final TntPrimeCause cause;
    private final @Nullable Entity source;
    private final @Nullable Block sourceBlock;

    /**
     * Creates a cancellable event for a TNT block about to be primed.
     *
     * @param block the TNT block
     * @param cause the trigger
     * @param source the direct priming entity, or {@code null}
     * @param sourceBlock the block responsible for priming, or {@code null}
     */
    public TntPrimeEvent(Block block, TntPrimeCause cause, @Nullable Entity source, @Nullable Block sourceBlock) {
        super(Objects.requireNonNull(block, "block"));
        this.cause = Objects.requireNonNull(cause, "cause");
        this.source = source;
        this.sourceBlock = sourceBlock;
    }

    /**
     * Returns the trigger that primed the block.
     */
    public TntPrimeCause getCause() {
        return this.cause;
    }

    /**
     * Returns the direct priming entity, or {@code null} when there is none.
     */
    public @Nullable Entity getSource() {
        return this.source;
    }

    /**
     * Returns the block that caused priming, or {@code null} if there was none.
     */
    public @Nullable Block getSourceBlock() {
        return this.sourceBlock;
    }
}
