package org.cloudburstmc.api.enchantment.behavior;

import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.util.behavior.Behavior;

public interface PostHurtBehavior {

    void doPostHurt(Behavior<PostHurtBehavior> behavior, ItemStack item, Entity victim, Entity attacker, int level);

    @FunctionalInterface
    interface Executor {
        void execute(ItemStack item, Entity victim, Entity attacker, int level);
    }
}
