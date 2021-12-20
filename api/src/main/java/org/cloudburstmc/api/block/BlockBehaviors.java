package org.cloudburstmc.api.block;

import org.cloudburstmc.api.block.behavior.*;
import org.cloudburstmc.api.block.material.MaterialType;
import org.cloudburstmc.api.blockentity.BlockEntityType;
import org.cloudburstmc.api.data.BehaviorKey;
import org.cloudburstmc.api.data.DataKey;
import org.cloudburstmc.api.util.Identifier;

import java.util.Optional;

public final class BlockBehaviors {

    private BlockBehaviors() {
    }

    public static final BehaviorKey<DescriptionBlockBehavior, DescriptionBlockBehavior.Executor> GET_DESCRIPTION_ID = DataKey.behavior(Identifier.fromString("get_description_id"), DescriptionBlockBehavior.class, DescriptionBlockBehavior.Executor.class);

    public static final BehaviorKey<AABBBlockBehavior, AABBBlockBehavior.Executor> GET_BOUNDING_BOX = DataKey.behavior(Identifier.fromString("get_bounding_box"), AABBBlockBehavior.class, AABBBlockBehavior.Executor.class);

    public static final BehaviorKey<BooleanBlockBehavior, BooleanBlockBehavior.Executor> CAN_BE_SILK_TOUCHED = DataKey.behavior(Identifier.fromString("can_be_silk_touched"), BooleanBlockBehavior.class, BooleanBlockBehavior.Executor.class);

    public static final BehaviorKey<BooleanBlockBehavior, BooleanBlockBehavior.Executor> CAN_BE_USED_IN_COMMANDS = DataKey.behavior(Identifier.fromString("can_be_used_in_commands"), BooleanBlockBehavior.class, BooleanBlockBehavior.Executor.class);

    public static final BehaviorKey<BooleanBlockBehavior, BooleanBlockBehavior.Executor> CAN_CONTAIN_LIQUID = DataKey.behavior(Identifier.fromString("can_contain_liquid"), BooleanBlockBehavior.class, BooleanBlockBehavior.Executor.class);

    public static final BehaviorKey<BooleanBlockBehavior, BooleanBlockBehavior.Executor> CAN_SPAWN_ON = DataKey.behavior(Identifier.fromString("can_spawn_on"), BooleanBlockBehavior.class, BooleanBlockBehavior.Executor.class);

    public static final BehaviorKey<FloatBlockBehavior, FloatBlockBehavior.Executor> GET_FRICTION = DataKey.behavior(Identifier.fromString("get_friction"), FloatBlockBehavior.class, FloatBlockBehavior.Executor.class);

    public static final BehaviorKey<FloatBlockBehavior, FloatBlockBehavior.Executor> GET_HARDNESS = DataKey.behavior(Identifier.fromString("get_hardness"), FloatBlockBehavior.class, FloatBlockBehavior.Executor.class);

    public static final BehaviorKey<IntBlockBehavior, IntBlockBehavior.Executor> GET_BURN_ODDS = DataKey.behavior(Identifier.fromString("get_burn_odds"), IntBlockBehavior.class, IntBlockBehavior.Executor.class);

    public static final BehaviorKey<IntBlockBehavior, IntBlockBehavior.Executor> GET_FLAME_ODDS = DataKey.behavior(Identifier.fromString("get_flame_odds"), IntBlockBehavior.class, IntBlockBehavior.Executor.class);

    public static final BehaviorKey<FloatBlockBehavior, FloatBlockBehavior.Executor> GET_GRAVITY = DataKey.behavior(Identifier.fromString("get_gravity"), FloatBlockBehavior.class, FloatBlockBehavior.Executor.class);

    public static final BehaviorKey<FloatBlockBehavior, FloatBlockBehavior.Executor> GET_THICKNESS = DataKey.behavior(Identifier.fromString("get_thickness"), FloatBlockBehavior.class, FloatBlockBehavior.Executor.class);

    public static final BehaviorKey<FloatBlockBehavior, FloatBlockBehavior.Executor> GET_DESTROY_SPEED = DataKey.behavior(Identifier.fromString("get_destroy_speed"), FloatBlockBehavior.class, FloatBlockBehavior.Executor.class);

    public static final BehaviorKey<ExpBlockBehavior, ExpBlockBehavior.Executor> GET_EXPERIENCE_DROP = DataKey.behavior(Identifier.fromString("get_experience_drop"), ExpBlockBehavior.class, ExpBlockBehavior.Executor.class);

//    public static final BehaviorKey<ToFloatTriFunction<Behavior, BlockState, RandomGenerator>, FloatBlockBehavior.Executor> GET_EXPLOSION_RESISTANCE = DataKey.behavior(Identifier.fromString("get_explosion_resistance"), ToFloatTriFunction.class, FloatBlockBehavior.Executor.class);

//    public static final BehaviorKey<BiFunction<Behavior, Block, ItemStack>, ItemStack> GET_SILK_TOUCH_ITEM_STACK = DataKey.behavior(Identifier.fromString("get_silk_touch_item_stack"), BiFunction.class, ItemStack.class);

//    public static final BehaviorKey<BasicBlockBehavior<BlockState>, BasicBlockBehavior.Executor<BlockState>> GET_STRIPPED_BLOCK = DataKey.behavior(Identifier.fromString("get_stripped_block"), BasicBlockBehavior.class, BasicBlockBehavior.Executor.class);

    public static final BehaviorKey<Optional<BlockEntityType<?>>, Optional<BlockEntityType<?>>> GET_BLOCK_ENTITY = DataKey.behavior(Identifier.fromString("get_block_entity"), Optional.class, Optional.class);

    public static final BehaviorKey<BooleanBlockBehavior, BooleanBlockBehavior.Executor> MAY_PICK = DataKey.behavior(Identifier.fromString("may_pick"), BooleanBlockBehavior.class, BooleanBlockBehavior.Executor.class);

    public static final BehaviorKey<MayPlaceBlockBehavior, MayPlaceBlockBehavior.Executor> MAY_PLACE = DataKey.behavior(Identifier.fromString("may_place"), MayPlaceBlockBehavior.class, MayPlaceBlockBehavior.Executor.class);

    public static final BehaviorKey<BooleanBlockBehavior, BooleanBlockBehavior.Executor> MAY_PLACE_ON = DataKey.behavior(Identifier.fromString("may_place_on"), BooleanBlockBehavior.class, BooleanBlockBehavior.Executor.class);

    public static final BehaviorKey<EntityBlockBehavior, EntityBlockBehavior.Executor> ON_DESTROY = DataKey.behavior(Identifier.fromString("on_destroy"), EntityBlockBehavior.class, EntityBlockBehavior.Executor.class);

    public static final BehaviorKey<NeighborBlockBehavior, NeighborBlockBehavior.Executor> ON_NEIGHBOUR_CHANGED = DataKey.behavior(Identifier.fromString("on_neighbour_changed"), NeighborBlockBehavior.class, NeighborBlockBehavior.Executor.class);

    public static final BehaviorKey<FallOnBlockBehavior, FallOnBlockBehavior.Executor> ON_FALL_ON = DataKey.behavior(Identifier.fromString("on_fall_on"), FallOnBlockBehavior.class, FallOnBlockBehavior.Executor.class);

    public static final BehaviorKey<ComplexBlockBehavior, ComplexBlockBehavior.Executor> ON_LIGHTNING_HIT = DataKey.behavior(Identifier.fromString("on_lightning_hit"), ComplexBlockBehavior.class, ComplexBlockBehavior.Executor.class);

    public static final BehaviorKey<ComplexBlockBehavior, ComplexBlockBehavior.Executor> ON_PLACE = DataKey.behavior(Identifier.fromString("on_place"), ComplexBlockBehavior.class, ComplexBlockBehavior.Executor.class);

    public static final BehaviorKey<EntityBlockBehavior, EntityBlockBehavior.Executor> ON_PROJECTILE_HIT = DataKey.behavior(Identifier.fromString("on_projectile_hit"), EntityBlockBehavior.class, EntityBlockBehavior.Executor.class);

    public static final BehaviorKey<ComplexBlockBehavior, ComplexBlockBehavior.Executor> ON_REDSTONE_UPDATE = DataKey.behavior(Identifier.fromString("on_redstone_update"), ComplexBlockBehavior.class, ComplexBlockBehavior.Executor.class);

    public static final BehaviorKey<ComplexBlockBehavior, ComplexBlockBehavior.Executor> ON_REMOVE = DataKey.behavior(Identifier.fromString("on_remove"), ComplexBlockBehavior.class, ComplexBlockBehavior.Executor.class);

    public static final BehaviorKey<TickBlockBehavior, TickBlockBehavior.Executor> ON_RANDOM_TICK = DataKey.behavior(Identifier.fromString("on_random_tick"), TickBlockBehavior.class, TickBlockBehavior.Executor.class);

    public static final BehaviorKey<Boolean, Boolean> CAN_RANDOM_TICK = DataKey.behavior(Identifier.fromString("can_random_tick"), Boolean.class);

    public static final BehaviorKey<TickBlockBehavior, TickBlockBehavior.Executor> ON_TICK = DataKey.behavior(Identifier.fromString("on_tick"), TickBlockBehavior.class, TickBlockBehavior.Executor.class);

    public static final BehaviorKey<UseBlockBehavior, BooleanBlockBehavior.Executor> USE = DataKey.behavior(Identifier.fromString("use"), UseBlockBehavior.class, BooleanBlockBehavior.Executor.class);

    public static final BehaviorKey<EntityBlockBehavior, EntityBlockBehavior.Executor> ON_STAND_ON = DataKey.behavior(Identifier.fromString("on_stand_on"), EntityBlockBehavior.class, EntityBlockBehavior.Executor.class);

    public static final BehaviorKey<EntityBlockBehavior, EntityBlockBehavior.Executor> ON_STEP_ON = DataKey.behavior(Identifier.fromString("on_step_on"), EntityBlockBehavior.class, EntityBlockBehavior.Executor.class);

    public static final BehaviorKey<EntityBlockBehavior, EntityBlockBehavior.Executor> ON_STEP_OFF = DataKey.behavior(Identifier.fromString("on_step_off"), EntityBlockBehavior.class, EntityBlockBehavior.Executor.class);

    public static final BehaviorKey<MaterialType, MaterialType> GET_MATERIAL = DataKey.behavior(Identifier.fromString("get_material"), MaterialType.class, MaterialType.class);

    public static final BehaviorKey<ResourceBlockBehavior, ResourceBlockBehavior.Executor> GET_SILK_TOUCH_RESOURCE = DataKey.behavior(Identifier.fromString("get_silk_touch_resource"), ResourceBlockBehavior.class, ResourceBlockBehavior.Executor.class);

    public static final BehaviorKey<ResourceBlockBehavior, ResourceBlockBehavior.Executor> GET_RESOURCE_ITEM = DataKey.behavior(Identifier.fromString("get_resource_item"), ResourceBlockBehavior.class, ResourceBlockBehavior.Executor.class);

    public static final BehaviorKey<ResourceCountBlockBehavior, ResourceCountBlockBehavior.Executor> GET_RESOURCE_COUNT = DataKey.behavior(Identifier.fromString("get_resource_count"), ResourceCountBlockBehavior.class, ResourceCountBlockBehavior.Executor.class);

    public static final BehaviorKey<Float, Float> GET_TRANSLUCENCY = DataKey.behavior(Identifier.fromString("get_translucency"), Float.class);

    public static final BehaviorKey<Boolean, Boolean> IS_SOLID = DataKey.behavior(Identifier.fromString("is_solid"), Boolean.class);

    public static final BehaviorKey<Boolean, Boolean> IS_LIQUID = DataKey.behavior(Identifier.fromString("is_liquid"), Boolean.class);

    public static final BehaviorKey<Boolean, Boolean> IS_TOP_SOLID = DataKey.behavior(Identifier.fromString("is_top_solid"), Boolean.class);

    public static final BehaviorKey<Boolean, Boolean> IS_STAIRS = DataKey.behavior(Identifier.fromString("is_stairs"), Boolean.class);

    public static final BehaviorKey<Boolean, Boolean> IS_SLAB = DataKey.behavior(Identifier.fromString("is_slab"), Boolean.class);

    public static final BehaviorKey<Boolean, Boolean> IS_REPLACEABLE = DataKey.behavior(Identifier.fromString("is_replaceable"), Boolean.class);

    public static final BehaviorKey<Boolean, Boolean> IS_SUPER_HOT = DataKey.behavior(Identifier.fromString("is_super_hot"), Boolean.class);

    public static final BehaviorKey<Boolean, Boolean> IS_FLAMMABLE = DataKey.behavior(Identifier.fromString("is_flammable"), Boolean.class);

    public static final BehaviorKey<ColorBlockBehavior, ColorBlockBehavior.Executor> GET_COLOR = DataKey.behavior(Identifier.fromString("get_color"), ColorBlockBehavior.class, ColorBlockBehavior.Executor.class);

    public static final BehaviorKey<Integer, Integer> GET_LIGHT = DataKey.behavior(Identifier.fromString("get_light"), Integer.class);

    public static final BehaviorKey<Boolean, Boolean> IS_ALWAYS_DESTROYABLE = DataKey.behavior(Identifier.fromString("is_always_destroyable"), Boolean.class);

    public static final BehaviorKey<Integer, Integer> GET_TICK_DELAY = DataKey.behavior(Identifier.fromString("get_tick_delay"), Integer.class);

    public static final BehaviorKey<SurviveBlockBehavior, SurviveBlockBehavior.Executor> CAN_SURVIVE = DataKey.behavior(Identifier.fromString("can_survive"), SurviveBlockBehavior.class, SurviveBlockBehavior.Executor.class);

    public static final BehaviorKey<ComplexBlockBehavior, ComplexBlockBehavior.Executor> CHECK_ALIVE = DataKey.behavior(Identifier.fromString("check_alive"), ComplexBlockBehavior.class, ComplexBlockBehavior.Executor.class);

    public static final BehaviorKey<Boolean, Boolean> IS_FLOODABLE = DataKey.behavior(Identifier.fromString("is_floodable"), Boolean.class);

    public static final BehaviorKey<Boolean, Boolean> CAN_INSTATICK = DataKey.behavior(Identifier.fromString("is_instaticking"), Boolean.class);

    public static final BehaviorKey<BooleanBlockBehavior, BooleanBlockBehavior.Executor> CAN_SLIDE = DataKey.behavior(Identifier.fromString("can_slide"), BooleanBlockBehavior.class, BooleanBlockBehavior.Executor.class);

    public static final BehaviorKey<BooleanBlockBehavior, BooleanBlockBehavior.Executor> IS_FREE_TO_FALL = DataKey.behavior(Identifier.fromString("is_free_to_fall"), BooleanBlockBehavior.class, BooleanBlockBehavior.Executor.class);

    public static final BehaviorKey<ComplexBlockBehavior, ComplexBlockBehavior.Executor> START_FALLING = DataKey.behavior(Identifier.fromString("start_falling"), ComplexBlockBehavior.class, ComplexBlockBehavior.Executor.class);
}
