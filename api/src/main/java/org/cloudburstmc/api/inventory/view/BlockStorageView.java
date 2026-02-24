package org.cloudburstmc.api.inventory.view;

import org.cloudburstmc.api.blockentity.BlockEntity;

/**
 * Represents the item slots of a storage container that is backed by a real block entity
 * in the world (chest, barrel, or shulker box).
 *
 * <p>This interface combines {@link StorageView} (the 27-slot storage contract) with
 * {@link BlockSlotGroup} (world-position and block-entity access).  Because three different
 * block-entity types can back a storage view (chest, barrel, shulker box), the type parameter
 * is left at the {@link BlockEntity} upper bound; use {@code instanceof} or
 * {@link BlockSlotGroup#getBlock()} to distinguish them at runtime.</p>
 *
 * <p>Virtual containers created by plugins via
 * {@link org.cloudburstmc.api.player.Player#createVirtualChest(String)} return the plain
 * {@link StorageView} because they have no backing block entity.</p>
 */
public interface BlockStorageView extends StorageView, BlockSlotGroup<BlockEntity> {
}
