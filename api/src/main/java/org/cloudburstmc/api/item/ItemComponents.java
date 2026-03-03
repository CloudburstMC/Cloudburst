package org.cloudburstmc.api.item;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.block.component.BooleanTypeHandler;
import org.cloudburstmc.api.block.component.FloatTypeHandler;
import org.cloudburstmc.api.data.ComponentType;
import org.cloudburstmc.api.item.component.*;

/**
 * Standard {@link ComponentType} constants for item behavioral components.
 */
@SuppressWarnings("rawtypes")
@UtilityClass
public class ItemComponents {
    public static final ComponentType<BooleanTypeHandler> ALLOW_OFFHAND = ComponentType.of("allow_offhand", BooleanTypeHandler.class);
    public static final ComponentType<BooleanItemHandler> CAN_BE_PLACED = ComponentType.of("can_be_placed", BooleanItemHandler.class);
    public static final ComponentType<CanBePlacedOnHandler> CAN_BE_PLACED_ON = ComponentType.of("can_be_placed_on", CanBePlacedOnHandler.class);
    public static final ComponentType<BooleanTypeHandler> CAN_BE_CHARGED = ComponentType.of("can_be_charged", BooleanTypeHandler.class);
    public static final ComponentType<BooleanTypeHandler> CAN_BE_DEPLETED = ComponentType.of("can_be_depleted", BooleanTypeHandler.class);
    public static final ComponentType<BooleanItemHandler> CAN_BE_USED = ComponentType.of("can_be_used", BooleanItemHandler.class);
    public static final ComponentType<CanDestroyHandler> CAN_DESTROY = ComponentType.of("can_destroy", CanDestroyHandler.class);
    public static final ComponentType<BooleanTypeHandler> CAN_DESTROY_IN_CREATIVE = ComponentType.of("can_destroy_in_creative", BooleanTypeHandler.class);
    public static final ComponentType<BooleanTypeHandler> DAMAGEABLE = ComponentType.of("damageable", BooleanTypeHandler.class);
    public static final ComponentType<FloatTypeHandler> FUEL_DURATION = ComponentType.of("fuel_duration", FloatTypeHandler.class);
    public static final ComponentType<FloatItemHandler> GET_ATTACK_DAMAGE_BONUS = ComponentType.of("get_attack_damage_bonus", FloatItemHandler.class);
    public static final ComponentType<GetItemHandler> GET_BLOCK = ComponentType.of("get_block", GetItemHandler.class);
    public static final ComponentType<DamageChanceHandler> GET_DAMAGE_CHANCE = ComponentType.of("get_damage_chance", DamageChanceHandler.class);
    public static final ComponentType<DestroySpeedHandler> GET_DESTROY_SPEED = ComponentType.of("get_destroy_speed", DestroySpeedHandler.class);
    public static final ComponentType<FloatItemHandler> GET_DESTROY_SPEED_BONUS = ComponentType.of("get_destroy_speed_bonus", FloatItemHandler.class);
    public static final ComponentType<IntItemHandler> GET_MAX_DAMAGE = ComponentType.of("get_max_damage", IntItemHandler.class);
    public static final ComponentType<IntItemHandler> GET_MAX_STACK_SIZE = ComponentType.of("get_max_stack_size", IntItemHandler.class);
    public static final ComponentType<BooleanItemHandler> IS_TOOL = ComponentType.of("is_tool", BooleanItemHandler.class);
    public static final ComponentType<MineBlockHandler> MINE_BLOCK = ComponentType.of("mine_block", MineBlockHandler.class);
    public static final ComponentType<DamageItemHandler> ON_DAMAGE = ComponentType.of("on_damage", DamageItemHandler.class);
    public static final ComponentType<UseOnHandler> USE_ON = ComponentType.of("use_on", UseOnHandler.class);
}
