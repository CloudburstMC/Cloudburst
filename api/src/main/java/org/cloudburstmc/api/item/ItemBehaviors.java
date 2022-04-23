package org.cloudburstmc.api.item;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.data.BehaviorKey;
import org.cloudburstmc.api.data.DataKey;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.api.util.behavior.IntBehavior;

@UtilityClass
public class ItemBehaviors {

    public static final BehaviorKey<IntBehavior, IntBehavior.Executor> GET_MAX_STACK_SIZE = DataKey.behavior(Identifier.fromString("get_max_stack_size"), IntBehavior.class, IntBehavior.Executor.class);

    public static final BehaviorKey<IntBehavior, IntBehavior.Executor> GET_MAX_DURABILITY = DataKey.behavior(Identifier.fromString("get_max_durability"), IntBehavior.class, IntBehavior.Executor.class);
}
