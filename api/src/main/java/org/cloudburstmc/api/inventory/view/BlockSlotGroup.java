package org.cloudburstmc.api.inventory.view;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.blockentity.BlockEntity;

/**
 * A {@link SlotGroup} backed by a block in the world.
 *
 * <p>The type parameter {@code E} is the specific {@link BlockEntity} type that backs this slot
 * group, or (for blocks that have no block entity at all) the marker subtype
 * {@link StatelessBlockSlotGroup} should be used instead; it always returns {@code null} from
 * {@link #getBlockEntity()} as a default method and removes the type parameter.
 * Stateless block views include crafting table, smithing table, loom, cartography table, and
 * stonecutter. When the slot group can be backed by one of several block-entity types
 * (chest, barrel, shulker box), use {@link BlockEntity} as the type parameter.</p>
 *
 * @param <E> the block entity type backing this slot group
 */
public interface BlockSlotGroup<E extends BlockEntity> extends SlotGroup {

    /**
     * Returns the block backing this slot group.
     *
     * @return the block
     */
    Block getBlock();

    /**
     * Returns the block entity backing this slot group.
     *
     * <p>May return {@code null} for blocks that have no persistent block entity
     * (e.g. crafting table, smithing table) or if the block entity has been removed.</p>
     *
     * <p><strong>Self-reference contract:</strong> when a block-entity interface itself
     * extends {@code BlockSlotGroup} (e.g.
     * {@link org.cloudburstmc.api.blockentity.Furnace Furnace} extends
     * {@code BlockSlotGroup<Furnace>}), the server implementation returns {@code this}.
     * This is intentional and valid; the block entity <em>is</em> the slot group.</p>
     *
     * @return the block entity, or {@code null}
     */
    E getBlockEntity();
}
