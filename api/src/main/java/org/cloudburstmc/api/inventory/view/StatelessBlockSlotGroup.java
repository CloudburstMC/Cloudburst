package org.cloudburstmc.api.inventory.view;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.blockentity.BlockEntity;

/**
 * A {@link BlockSlotGroup} for blocks that have <em>no</em> persistent block entity.
 *
 * <p>Several interactive blocks (crafting table, smithing table, loom, cartography table,
 * stonecutter) open a container UI but store no data between interactions — they have no
 * {@link BlockEntity} in the world. This interface makes that contract explicit at the
 * type level by overriding {@link BlockSlotGroup#getBlockEntity()} to always return
 * {@code null}.</p>
 *
 * <p>Block-entity-backed slot groups should extend {@link BlockSlotGroup} directly (or
 * one of its typed subtypes such as {@code BlockFurnaceView}, {@code BlockHopperView},
 * etc.) rather than this interface.</p>
 *
 * @see BlockSlotGroup
 */
public interface StatelessBlockSlotGroup extends BlockSlotGroup<BlockEntity> {

    /**
     * Always returns {@code null} — this block has no persistent block entity.
     *
     * @return {@code null}
     */
    @Override
    @Nullable
    default BlockEntity getBlockEntity() {
        return null;
    }
}
