package org.cloudburstmc.api.item;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.block.component.BooleanTypeHandler;
import org.cloudburstmc.api.block.component.FloatTypeHandler;
import org.cloudburstmc.api.data.ComponentType;
import org.cloudburstmc.api.entity.damage.DamageType;
import org.cloudburstmc.api.item.component.*;

/**
 * Standard {@link ComponentType} constants for item behavioral components.
 */
@SuppressWarnings("rawtypes")
@UtilityClass
public class ItemBehaviors {

    /**
     * Determines whether this item type may occupy the off-hand slot.
     */
    public static final ComponentType<BooleanTypeHandler> ALLOW_OFFHAND = ComponentType.of("allow_offhand", BooleanTypeHandler.class);

    /**
     * Defines the protection supplied while this item is equipped.
     */
    public static final ComponentType<ArmorComponent> ARMOR = ComponentType.of("armor", ArmorComponent.class);

    /**
     * Defines the damage type used for melee attacks with this item.
     */
    public static final ComponentType<DamageType> ATTACK_DAMAGE_TYPE = ComponentType.of("attack_damage_type", DamageType.class);

    /**
     * Determines whether an item stack represents a placeable item.
     */
    public static final ComponentType<BooleanItemHandler> CAN_BE_PLACED = ComponentType.of("can_be_placed", BooleanItemHandler.class);

    /**
     * Determines whether an item may be placed against a target block.
     */
    public static final ComponentType<CanBePlacedOnHandler> CAN_BE_PLACED_ON = ComponentType.of("can_be_placed_on", CanBePlacedOnHandler.class);

    /**
     * Determines whether this item type supports a charged state.
     */
    public static final ComponentType<BooleanTypeHandler> CAN_BE_CHARGED = ComponentType.of("can_be_charged", BooleanTypeHandler.class);

    /**
     * Determines whether this item type can lose durability through use.
     */
    public static final ComponentType<BooleanTypeHandler> CAN_BE_DEPLETED = ComponentType.of("can_be_depleted", BooleanTypeHandler.class);

    /**
     * Determines whether an item may destroy a target block.
     */
    public static final ComponentType<CanDestroyHandler> CAN_DESTROY = ComponentType.of("can_destroy", CanDestroyHandler.class);

    /**
     * Determines whether this item type may destroy blocks in creative mode.
     */
    public static final ComponentType<BooleanTypeHandler> CAN_DESTROY_IN_CREATIVE = ComponentType.of("can_destroy_in_creative", BooleanTypeHandler.class);

    /**
     * Determines whether a specific enchantment may be applied to an item.
     */
    public static final ComponentType<CanEnchantWithHandler> CAN_ENCHANT_WITH = ComponentType.of("can_enchant_with", CanEnchantWithHandler.class);

    /**
     * Determines whether a material may be used to repair an item.
     */
    public static final ComponentType<CanRepairWithHandler> CAN_REPAIR_WITH = ComponentType.of("can_repair_with", CanRepairWithHandler.class);

    /**
     * Determines whether enchantments may be stored on this item type.
     */
    public static final ComponentType<BooleanTypeHandler> CAN_STORE_ENCHANTMENTS = ComponentType.of("can_store_enchantments", BooleanTypeHandler.class);

    /**
     * Determines whether this item type has durability.
     */
    public static final ComponentType<BooleanTypeHandler> DAMAGEABLE = ComponentType.of("damageable", BooleanTypeHandler.class);

    /**
     * Applies the behavior that occurs when an item's use duration completes.
     */
    public static final ComponentType<FinishUseHandler> FINISH_USE = ComponentType.of("finish_use", FinishUseHandler.class);

    /**
     * Supplies the burn duration when this item type is used as fuel.
     */
    public static final ComponentType<FloatTypeHandler> FUEL_DURATION = ComponentType.of("fuel_duration", FloatTypeHandler.class);

    /**
     * Calculates the base melee damage dealt with this item.
     */
    public static final ComponentType<FloatItemHandler> GET_ATTACK_DAMAGE = ComponentType.of("get_attack_damage", FloatItemHandler.class);

    /**
     * Calculates the durability consumed by a successful melee attack.
     */
    public static final ComponentType<IntItemHandler> GET_ATTACK_DURABILITY_DAMAGE = ComponentType.of("get_attack_durability_damage", IntItemHandler.class);

    /**
     * Obtains the block state represented by an item, if any.
     *
     * <p>The handler returns an {@link java.util.Optional} containing the block state.</p>
     */
    public static final ComponentType<GetItemHandler> GET_BLOCK = ComponentType.of("get_block", GetItemHandler.class);

    /**
     * Calculates the percentage chance that one point of durability damage is applied.
     */
    public static final ComponentType<DamageChanceHandler> GET_DAMAGE_CHANCE = ComponentType.of("get_damage_chance", DamageChanceHandler.class);

    /**
     * Obtains the slot occupied when an item is equipped, or {@code null} if it is not equipment.
     */
    public static final ComponentType<GetEquipmentSlotHandler> GET_EQUIPMENT_SLOT = ComponentType.of("get_equipment_slot", GetEquipmentSlotHandler.class);

    /**
     * Calculates an item's maximum durability.
     */
    public static final ComponentType<IntItemHandler> GET_MAX_DAMAGE = ComponentType.of("get_max_damage", IntItemHandler.class);

    /**
     * Calculates an item's maximum stack size.
     */
    public static final ComponentType<IntItemHandler> GET_MAX_STACK_SIZE = ComponentType.of("get_max_stack_size", IntItemHandler.class);

    /**
     * Obtains an item's mining behavior, or {@code null} if it is not a tool.
     */
    public static final ComponentType<GetToolHandler> GET_TOOL = ComponentType.of("get_tool", GetToolHandler.class);

    /**
     * Applies the item changes caused by successfully mining a block.
     */
    public static final ComponentType<MineBlockHandler> MINE_BLOCK = ComponentType.of("mine_block", MineBlockHandler.class);

    /**
     * Applies durability damage and returns the resulting item stack.
     */
    public static final ComponentType<DamageItemHandler> ON_DAMAGE = ComponentType.of("on_damage", DamageItemHandler.class);

    /**
     * Applies an item's behavior when its holder releases the use action, if one is registered.
     */
    public static final ComponentType<ReleaseUseHandler> RELEASE_USE = ComponentType.of("release_use", ReleaseUseHandler.class);

    /**
     * Identifies the entity type created by a spawn egg.
     */
    public static final ComponentType<SpawnEggComponent> SPAWN_EGG = ComponentType.of("spawn_egg", SpawnEggComponent.class);

    /**
     * Performs a piercing weapon's stab action.
     */
    public static final ComponentType<StabHandler> STAB = ComponentType.of("stab", StabHandler.class);

    /**
     * Applies an item's general use behavior and returns the resulting item stack.
     */
    public static final ComponentType<UseHandler> USE = ComponentType.of("use", UseHandler.class);

    /**
     * Defines how many ticks an item must be used before its use completes.
     */
    public static final ComponentType<Integer> USE_DURATION_TICKS = ComponentType.of("use_duration_ticks", Integer.class);

    /**
     * Applies the behavior for using an item on a block and returns the resulting item stack.
     */
    public static final ComponentType<UseOnHandler> USE_ON = ComponentType.of("use_on", UseOnHandler.class);

    /**
     * Runs while an item is being used, before its duration completes, if one is registered.
     */
    public static final ComponentType<UseTickHandler> USE_TICK = ComponentType.of("use_tick", UseTickHandler.class);
}
