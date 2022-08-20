package org.cloudburstmc.api.item;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.behavior.BooleanBlockBehavior;
import org.cloudburstmc.api.data.BehaviorKey;
import org.cloudburstmc.api.data.DataKey;
import org.cloudburstmc.api.item.behavior.*;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.api.util.behavior.FloatBehavior;
import org.cloudburstmc.api.util.behavior.IntBehavior;

import java.util.Optional;

@UtilityClass
public class ItemBehaviors {

    public static final BehaviorKey<IntBehavior, IntBehavior.Executor> GET_MAX_STACK_SIZE = DataKey.behavior(Identifier.fromString("get_max_stack_size"), IntBehavior.class, IntBehavior.Executor.class);

    public static final BehaviorKey<Boolean, Boolean> IS_DAMAGEABLE = DataKey.behavior(Identifier.fromString("is_damageable"), Boolean.class);
    public static final BehaviorKey<IntBehavior, IntBehavior.Executor> GET_MAX_DAMAGE = DataKey.behavior(Identifier.fromString("get_max_damage"), IntBehavior.class, IntBehavior.Executor.class);

    public static final BehaviorKey<MineBlockBehavior, MineBlockBehavior.Executor> MINE_BLOCK = DataKey.behavior(Identifier.fromString("mine_block"), MineBlockBehavior.class, MineBlockBehavior.Executor.class);

    public static final BehaviorKey<DamageItemBehavior, DamageItemBehavior.Executor> ON_DAMAGE = DataKey.behavior(Identifier.fromString("on_damage"), DamageItemBehavior.class, DamageItemBehavior.Executor.class);

    public static final BehaviorKey<BooleanBlockBehavior, BooleanBlockBehavior.Executor> CAN_DESTROY = DataKey.behavior(Identifier.fromString("can_destroy"), BooleanBlockBehavior.class, BooleanBlockBehavior.Executor.class);

    public static final BehaviorKey<DestroySpeedBehavior, DestroySpeedBehavior.Executor> GET_DESTROY_SPEED = DataKey.behavior(Identifier.fromString("get_destroy_speed"), DestroySpeedBehavior.class, DestroySpeedBehavior.Executor.class);

    public static final BehaviorKey<FloatItemBehavior, FloatItemBehavior.Executor> GET_DESTROY_SPEED_BONUS = DataKey.behavior(Identifier.fromString("get_destroy_speed_bonus"), FloatItemBehavior.class, FloatItemBehavior.Executor.class);

    public static final BehaviorKey<Boolean, Boolean> CAN_DESTROY_IN_CREATIVE = DataKey.behavior(Identifier.fromString("can_destroy_in_creative"), Boolean.class);

    public static final BehaviorKey<DamageChanceBehavior, DamageChanceBehavior.Executor> GET_DAMAGE_CHANCE = DataKey.behavior(Identifier.fromString("get_damage_chance"), DamageChanceBehavior.class, DamageChanceBehavior.Executor.class);

    public static final BehaviorKey<BooleanItemBehavior, BooleanItemBehavior.Executor> CAN_BE_USED = DataKey.behavior(Identifier.fromString("can_be_used"), BooleanItemBehavior.class, BooleanItemBehavior.Executor.class);
    public static final BehaviorKey<UseOnBehavior, UseOnBehavior.Executor> USE_ON = DataKey.behavior(Identifier.fromString("use_on"), UseOnBehavior.class, UseOnBehavior.Executor.class);

    public static final BehaviorKey<Float, Float> GET_FUEL_DURATION = DataKey.behavior(Identifier.fromString("get_fuel_duration"), Float.class);

    public static final BehaviorKey<FloatBehavior, FloatBehavior.Executor> GET_ATTACH_DAMAGE = DataKey.behavior(Identifier.fromString("get_attack_damage"), FloatBehavior.class, FloatBehavior.Executor.class);

    public static final BehaviorKey<BooleanItemBehavior, BooleanItemBehavior.Executor> IS_TOOL = DataKey.behavior(Identifier.fromString("is_tool"), BooleanItemBehavior.class, BooleanItemBehavior.Executor.class);

    public static final BehaviorKey<BooleanItemBehavior, BooleanItemBehavior.Executor> CAN_BE_PLACED = DataKey.behavior(Identifier.fromString("can_be_placed"), BooleanItemBehavior.class, BooleanItemBehavior.Executor.class);

    public static final BehaviorKey<CanBePlacedOnBehavior, CanBePlacedOnBehavior.Executor> CAN_BE_PLACED_ON = DataKey.behavior(Identifier.fromString("can_be_placed_on"), CanBePlacedOnBehavior.class, CanBePlacedOnBehavior.Executor.class);

    public static final BehaviorKey<GetItemBehavior<Optional<BlockState>>, GetItemBehavior.Executor<Optional<BlockState>>> GET_BLOCK = DataKey.behavior(Identifier.fromString("get_block"), GetItemBehavior.class, GetItemBehavior.Executor.class);

    public static final BehaviorKey<Boolean, Boolean> ALLOW_OFFHAND = DataKey.behavior(Identifier.fromString("allow_offhand"), Boolean.class);

    public static final BehaviorKey<Boolean, Boolean> CAN_BE_CHARGED = DataKey.behavior(Identifier.fromString("can_be_charged"), Boolean.class);

    public static final BehaviorKey<Boolean, Boolean> CAN_BE_DEPLETED = DataKey.behavior(Identifier.fromString("can_be_depleted"), Boolean.class);

    public static final BehaviorKey<Integer, Integer> GET_ATTACK_DAMAGE = DataKey.behavior(Identifier.fromString("get_attack_damage"), Integer.class);

}
