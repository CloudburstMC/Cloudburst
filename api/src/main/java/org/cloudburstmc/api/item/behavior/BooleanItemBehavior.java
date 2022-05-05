package org.cloudburstmc.api.item.behavior;

import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.util.behavior.Behavior;

public interface BooleanItemBehavior {

    boolean get(Behavior<Executor> behavior, ItemStack itemStack);

    @FunctionalInterface
    interface Executor {

        boolean execute(ItemStack itemStack);
    }
}
