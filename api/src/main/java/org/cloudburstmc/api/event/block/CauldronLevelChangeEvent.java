package org.cloudburstmc.api.event.block;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.blockentity.CauldronContents;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.event.Cancellable;

import java.util.Objects;

/**
 * Called before gameplay changes a cauldron's liquid, fill level, potion or dye.
 * Cancellation also prevents the associated item exchange or extinguishing action.
 */
public class CauldronLevelChangeEvent extends BlockEvent implements Cancellable {

    private final @Nullable Entity entity;
    private final CauldronChangeReason reason;
    private BlockState newState;
    private CauldronContents newContents;

    /**
     * @param block       the cauldron before the change
     * @param entity      the acting entity, or null for natural changes
     * @param reason      the cause of the change
     * @param newState    the proposed block state
     * @param newContents the proposed potion and dye contents
     */
    public CauldronLevelChangeEvent(Block block, @Nullable Entity entity, CauldronChangeReason reason, BlockState newState, CauldronContents newContents) {
        super(block);
        this.entity = entity;
        this.reason = Objects.requireNonNull(reason, "reason");
        this.newState = Objects.requireNonNull(newState, "newState");
        this.newContents = Objects.requireNonNull(newContents, "newContents");
    }

    /**
     * @return the acting entity, or null for natural changes
     */
    public @Nullable Entity getEntity() {
        return this.entity;
    }

    /**
     * @return the cause of the change
     */
    public CauldronChangeReason getReason() {
        return this.reason;
    }

    /**
     * @return the proposed block state
     */
    public BlockState getNewState() {
        return this.newState;
    }

    /**
     * @param newState the block state to apply if the event is accepted
     */
    public void setNewState(BlockState newState) {
        this.newState = Objects.requireNonNull(newState, "newState");
    }

    /**
     * @return the proposed potion and dye contents
     */
    public CauldronContents getNewContents() {
        return this.newContents;
    }

    /**
     * @param contents the potion and dye contents to apply if the event is accepted
     */
    public void setNewContents(CauldronContents contents) {
        this.newContents = Objects.requireNonNull(contents, "contents");
    }
}
