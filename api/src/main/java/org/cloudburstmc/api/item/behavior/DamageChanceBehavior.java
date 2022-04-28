package org.cloudburstmc.api.item.behavior;

import org.cloudburstmc.api.util.behavior.Behavior;

public interface DamageChanceBehavior {

    int getDamageChance(Behavior<Executor> executor, int unbreaking);

    interface Executor {

        int execute(int unbreaking);
    }
}
