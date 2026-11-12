package org.cloudburstmc.api.event.entity;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.misc.Painting;
import org.cloudburstmc.api.event.Cancellable;

/**
 * Called before a painting is broken. Cancelling leaves it attached.
 */
public class HangingBreakEvent extends EntityEvent implements Cancellable {

    private final HangingBreakCause cause;
    private final @Nullable Entity remover;

    public HangingBreakEvent(Painting painting, HangingBreakCause cause, @Nullable Entity remover) {
        this.entity = painting;
        this.cause = cause;
        this.remover = remover;
    }

    @Override
    public Painting getEntity() {
        return (Painting) super.getEntity();
    }

    /**
     * Returns why the painting is being broken.
     */
    public HangingBreakCause getCause() {
        return this.cause;
    }

    /**
     * Returns the responsible entity, if one is known.
     */
    public @Nullable Entity getRemover() {
        return this.remover;
    }

}
