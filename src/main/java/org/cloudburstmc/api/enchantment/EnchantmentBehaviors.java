package org.cloudburstmc.api.enchantment;

import org.cloudburstmc.api.data.BehaviorKey;
import org.cloudburstmc.api.data.DataKey;
import org.cloudburstmc.api.enchantment.behavior.DamageBonusBehavior;
import org.cloudburstmc.api.enchantment.behavior.DamageProtectionBehavior;
import org.cloudburstmc.api.enchantment.behavior.PostHurtBehavior;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.api.util.behavior.IntBehavior;

public final class EnchantmentBehaviors {

    public static final BehaviorKey<DamageBonusBehavior, DamageBonusBehavior.Executor> GET_DAMAGE_BONUS =
            DataKey.behavior(Identifier.fromString("get_damage_bonus"), DamageBonusBehavior.class, DamageBonusBehavior.Executor.class);

    public static final BehaviorKey<DamageProtectionBehavior, DamageProtectionBehavior.Executor> GET_DAMAGE_PROTECTION =
            DataKey.behavior(Identifier.fromString("get_damage_protection"), DamageProtectionBehavior.class, DamageProtectionBehavior.Executor.class);

    public static final BehaviorKey<PostHurtBehavior, PostHurtBehavior.Executor> DO_POST_HURT =
            DataKey.behavior(Identifier.fromString("do_post_hurt"), PostHurtBehavior.class, PostHurtBehavior.Executor.class);

    public static final BehaviorKey<IntBehavior, IntBehavior.Executor> GET_MIN_LEVEL =
            DataKey.behavior(Identifier.fromString("get_min_level"), IntBehavior.class, IntBehavior.Executor.class);

    public static final BehaviorKey<IntBehavior, IntBehavior.Executor> GET_MAX_LEVEL =
            DataKey.behavior(Identifier.fromString("get_max_level"), IntBehavior.class, IntBehavior.Executor.class);

    public static final BehaviorKey<IntBehavior, IntBehavior.Executor> GET_MIN_COST =
            DataKey.behavior(Identifier.fromString("get_min_cost"), IntBehavior.class, IntBehavior.Executor.class);

    public static final BehaviorKey<IntBehavior, IntBehavior.Executor> GET_MAX_COST =
            DataKey.behavior(Identifier.fromString("get_max_cost"), IntBehavior.class, IntBehavior.Executor.class);

    public static final BehaviorKey<IntBehavior, IntBehavior.Executor> GET_FREQUENCY =
            DataKey.behavior(Identifier.fromString("get_frequency"), IntBehavior.class, IntBehavior.Executor.class);
}
