package org.cloudburstmc.api.enchantment.behavior;

import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.util.behavior.Behavior;

public interface DamageBonusBehavior {

    float getDamageBonus(Behavior<DamageBonusBehavior> behavior, int level, Entity target);

    @FunctionalInterface
    interface Executor {
        float execute(int level, Entity target);
    }
}
