package org.cloudburstmc.api.item.component;

import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;

/**
 * Applies an item's behavior when used on a block.
 */
@FunctionalInterface
public interface UseOnHandler {

    /**
     * @param itemStack the item being used
     * @param entity the entity using the item
     * @param blockPosition the target block position
     * @param face the clicked face
     * @param clickPosition the click position within the block
     * @return the resulting item
     */
    ItemStack execute(ItemStack itemStack, Entity entity, Vector3i blockPosition, Direction face, Vector3f clickPosition);
}
