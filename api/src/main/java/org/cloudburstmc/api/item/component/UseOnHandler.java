package org.cloudburstmc.api.item.component;

import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;

@FunctionalInterface
public interface UseOnHandler {

    ItemStack execute(ItemStack itemStack, Entity entity, Vector3i blockPosition, Direction face, Vector3f clickPosition);
}
