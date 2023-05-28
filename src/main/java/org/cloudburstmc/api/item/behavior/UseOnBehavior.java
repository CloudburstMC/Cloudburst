package org.cloudburstmc.api.item.behavior;

import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.api.util.behavior.Behavior;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;

public interface UseOnBehavior {

    ItemStack useOn(Behavior<Executor> behavior, ItemStack itemStack, Entity entity, Vector3i blockPosition, Direction face, Vector3f clickPosition);

    @FunctionalInterface
    interface Executor {

        ItemStack execute(ItemStack itemStack, Entity entity, Vector3i blockPosition, Direction face, Vector3f clickPosition);
    }
}
