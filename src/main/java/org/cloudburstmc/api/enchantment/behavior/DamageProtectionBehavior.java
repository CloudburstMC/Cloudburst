package org.cloudburstmc.api.enchantment.behavior;

import org.cloudburstmc.api.event.entity.EntityDamageEvent;
import org.cloudburstmc.api.util.behavior.Behavior;

public interface DamageProtectionBehavior {

    int getDamageProtection(Behavior<DamageProtectionBehavior> behavior, int level, EntityDamageEvent event);

    @FunctionalInterface
    interface Executor {
        int execute(int level, EntityDamageEvent event);
    }
}
