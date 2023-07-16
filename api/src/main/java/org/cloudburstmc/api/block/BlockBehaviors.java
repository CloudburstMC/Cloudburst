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

    public static final BehaviorKey<DescriptionBlockBehavior, DescriptionBlockBehavior.Executor> GET_DESCRIPTION_ID = DataKey.behavior(Identifier.parse("get_description_id"), DescriptionBlockBehavior.class, DescriptionBlockBehavior.Executor.class);

    public static final BehaviorKey<AABBBlockBehavior, AABBBlockBehavior.Executor> GET_BOUNDING_BOX = DataKey.behavior(Identifier.parse("get_bounding_box"), AABBBlockBehavior.class, AABBBlockBehavior.Executor.class);

    public static final BehaviorKey<BooleanBlockBehavior, BooleanBlockBehavior.Executor> CAN_BE_SILK_TOUCHED = DataKey.behavior(Identifier.parse("can_be_silk_touched"), BooleanBlockBehavior.class, BooleanBlockBehavior.Executor.class);

    public static final BehaviorKey<BooleanBlockBehavior, BooleanBlockBehavior.Executor> CAN_BE_USED_IN_COMMANDS = DataKey.behavior(Identifier.parse("can_be_used_in_commands"), BooleanBlockBehavior.class, BooleanBlockBehavior.Executor.class);

    public static final BehaviorKey<BooleanBlockBehavior, BooleanBlockBehavior.Executor> CAN_CONTAIN_LIQUID = DataKey.behavior(Identifier.parse("can_contain_liquid"), BooleanBlockBehavior.class, BooleanBlockBehavior.Executor.class);

    public static final BehaviorKey<BooleanBlockBehavior, BooleanBlockBehavior.Executor> CAN_SPAWN_ON = DataKey.behavior(Identifier.parse("can_spawn_on"), BooleanBlockBehavior.class, BooleanBlockBehavior.Executor.class);

    public static final BehaviorKey<BooleanBlockBehavior, BooleanBlockBehavior.Executor> CAN_BE_USED = DataKey.behavior(Identifier.parse("can_be_used"), BooleanBlockBehavior.class, BooleanBlockBehavior.Executor.class);

    public static final BehaviorKey<FloatBlockBehavior, FloatBlockBehavior.Executor> GET_FRICTION = DataKey.behavior(Identifier.parse("get_friction"), FloatBlockBehavior.class, FloatBlockBehavior.Executor.class);

    public static final BehaviorKey<FloatBlockBehavior, FloatBlockBehavior.Executor> GET_HARDNESS = DataKey.behavior(Identifier.parse("get_hardness"), FloatBlockBehavior.class, FloatBlockBehavior.Executor.class);

    public static final BehaviorKey<IntBlockBehavior, IntBlockBehavior.Executor> GET_BURN_ODDS = DataKey.behavior(Identifier.parse("get_burn_odds"), IntBlockBehavior.class, IntBlockBehavior.Executor.class);

    public static final BehaviorKey<IntBlockBehavior, IntBlockBehavior.Executor> GET_FLAME_ODDS = DataKey.behavior(Identifier.parse("get_flame_odds"), IntBlockBehavior.class, IntBlockBehavior.Executor.class);

    public static final BehaviorKey<FloatBlockBehavior, FloatBlockBehavior.Executor> GET_GRAVITY = DataKey.behavior(Identifier.parse("get_gravity"), FloatBlockBehavior.class, FloatBlockBehavior.Executor.class);

    public static final BehaviorKey<FloatBlockBehavior, FloatBlockBehavior.Executor> GET_THICKNESS = DataKey.behavior(Identifier.parse("get_thickness"), FloatBlockBehavior.class, FloatBlockBehavior.Executor.class);

    public static final BehaviorKey<FloatBlockBehavior, FloatBlockBehavior.Executor> GET_DESTROY_SPEED = DataKey.behavior(Identifier.parse("get_destroy_speed"), FloatBlockBehavior.class, FloatBlockBehavior.Executor.class);

    public static final BehaviorKey<ExpBlockBehavior, ExpBlockBehavior.Executor> GET_EXPERIENCE_DROP = DataKey.behavior(Identifier.parse("get_experience_drop"), ExpBlockBehavior.class, ExpBlockBehavior.Executor.class);

//    public static final BehaviorKey<ToFloatTriFunction<Behavior, BlockState, RandomGenerator>, FloatBlockBehavior.Executor> GET_EXPLOSION_RESISTANCE = DataKey.behavior(Identifier.fromString("get_explosion_resistance"), ToFloatTriFunction.class, FloatBlockBehavior.Executor.class);

//    public static final BehaviorKey<BiFunction<Behavior, Block, ItemStack>, ItemStack> GET_SILK_TOUCH_ITEM_STACK = DataKey.behavior(Identifier.fromString("get_silk_touch_item_stack"), BiFunction.class, ItemStack.class);

//    public static final BehaviorKey<BasicBlockBehavior<BlockState>, BasicBlockBehavior.Executor<BlockState>> GET_STRIPPED_BLOCK = DataKey.behavior(Identifier.fromString("get_stripped_block"), BasicBlockBehavior.class, BasicBlockBehavior.Executor.class);

    public static final BehaviorKey<GenericBlockBehavior<Optional<BlockEntityType<?>>>, GenericBlockBehavior.Executor<Optional<BlockEntityType<?>>>> GET_BLOCK_ENTITY = DataKey.behavior(Identifier.parse("get_block_entity"), GenericBlockBehavior.class, GenericBlockBehavior.Executor.class);

    public static final BehaviorKey<BooleanBlockBehavior, BooleanBlockBehavior.Executor> MAY_PICK = DataKey.behavior(Identifier.parse("may_pick"), BooleanBlockBehavior.class, BooleanBlockBehavior.Executor.class);

    public static final BehaviorKey<MayPlaceBlockBehavior, MayPlaceBlockBehavior.Executor> MAY_PLACE = DataKey.behavior(Identifier.parse("may_place"), MayPlaceBlockBehavior.class, MayPlaceBlockBehavior.Executor.class);

    public static final BehaviorKey<BooleanBlockBehavior, BooleanBlockBehavior.Executor> MAY_PLACE_ON = DataKey.behavior(Identifier.parse("may_place_on"), BooleanBlockBehavior.class, BooleanBlockBehavior.Executor.class);

    public static final BehaviorKey<PlayerBlockBehavior, PlayerBlockBehavior.Executor> ON_DESTROY = DataKey.behavior(Identifier.parse("on_destroy"), PlayerBlockBehavior.class, PlayerBlockBehavior.Executor.class);

    public static final BehaviorKey<PlayerBlockBehavior, PlayerBlockBehavior.Executor> POST_DESTROY = DataKey.behavior(Identifier.parse("post_destroy"), PlayerBlockBehavior.class, PlayerBlockBehavior.Executor.class);

    public static final BehaviorKey<NeighborBlockBehavior, NeighborBlockBehavior.Executor> ON_NEIGHBOUR_CHANGED = DataKey.behavior(Identifier.parse("on_neighbour_changed"), NeighborBlockBehavior.class, NeighborBlockBehavior.Executor.class);

    public static final BehaviorKey<FallOnBlockBehavior, FallOnBlockBehavior.Executor> ON_FALL_ON = DataKey.behavior(Identifier.parse("on_fall_on"), FallOnBlockBehavior.class, FallOnBlockBehavior.Executor.class);

    public static final BehaviorKey<ComplexBlockBehavior, ComplexBlockBehavior.Executor> ON_LIGHTNING_HIT = DataKey.behavior(Identifier.parse("on_lightning_hit"), ComplexBlockBehavior.class, ComplexBlockBehavior.Executor.class);

    public static final BehaviorKey<PlaceBlockBehavior, PlaceBlockBehavior.Executor> ON_PLACE = DataKey.behavior(Identifier.parse("on_place"), PlaceBlockBehavior.class, PlaceBlockBehavior.Executor.class);

    public static final BehaviorKey<EntityBlockBehavior, EntityBlockBehavior.Executor> ON_PROJECTILE_HIT = DataKey.behavior(Identifier.parse("on_projectile_hit"), EntityBlockBehavior.class, EntityBlockBehavior.Executor.class);

    public static final BehaviorKey<ComplexBlockBehavior, ComplexBlockBehavior.Executor> ON_REDSTONE_UPDATE = DataKey.behavior(Identifier.parse("on_redstone_update"), ComplexBlockBehavior.class, ComplexBlockBehavior.Executor.class);

    public static final BehaviorKey<ComplexBlockBehavior, ComplexBlockBehavior.Executor> ON_REMOVE = DataKey.behavior(Identifier.parse("on_remove"), ComplexBlockBehavior.class, ComplexBlockBehavior.Executor.class);

    public static final BehaviorKey<TickBlockBehavior, TickBlockBehavior.Executor> ON_RANDOM_TICK = DataKey.behavior(Identifier.parse("on_random_tick"), TickBlockBehavior.class, TickBlockBehavior.Executor.class);

    public static final BehaviorKey<Boolean, Boolean> CAN_RANDOM_TICK = DataKey.behavior(Identifier.parse("can_random_tick"), Boolean.class);

    public static final BehaviorKey<TickBlockBehavior, TickBlockBehavior.Executor> ON_TICK = DataKey.behavior(Identifier.parse("on_tick"), TickBlockBehavior.class, TickBlockBehavior.Executor.class);

    public static final BehaviorKey<UseBlockBehavior, UseBlockBehavior.Executor> USE = DataKey.behavior(Identifier.parse("use"), UseBlockBehavior.class, UseBlockBehavior.Executor.class);

    public static final BehaviorKey<EntityBlockBehavior, EntityBlockBehavior.Executor> ON_STAND_ON = DataKey.behavior(Identifier.parse("on_stand_on"), EntityBlockBehavior.class, EntityBlockBehavior.Executor.class);

    public static final BehaviorKey<EntityBlockBehavior, EntityBlockBehavior.Executor> ON_STEP_ON = DataKey.behavior(Identifier.parse("on_step_on"), EntityBlockBehavior.class, EntityBlockBehavior.Executor.class);

    public static final BehaviorKey<EntityBlockBehavior, EntityBlockBehavior.Executor> ON_STEP_OFF = DataKey.behavior(Identifier.parse("on_step_off"), EntityBlockBehavior.class, EntityBlockBehavior.Executor.class);

    public static final BehaviorKey<MaterialType, MaterialType> GET_MATERIAL = DataKey.behavior(Identifier.parse("get_material"), MaterialType.class, MaterialType.class);

    public static final BehaviorKey<ResourceBlockBehavior, ResourceBlockBehavior.Executor> GET_SILK_TOUCH_RESOURCE = DataKey.behavior(Identifier.parse("get_silk_touch_resource"), ResourceBlockBehavior.class, ResourceBlockBehavior.Executor.class);

    public static final BehaviorKey<DropResourceBlockBehavior, DropResourceBlockBehavior.Executor> DROP_RESOURCE = DataKey.behavior(Identifier.parse("drop_resource"), DropResourceBlockBehavior.class, DropResourceBlockBehavior.Executor.class);

    public static final BehaviorKey<SpawnResourcesBlockBehavior, SpawnResourcesBlockBehavior.Executor> SPAWN_RESOURCES = DataKey.behavior(Identifier.parse("spawn_resources"), SpawnResourcesBlockBehavior.class, SpawnResourcesBlockBehavior.Executor.class);

    public static final BehaviorKey<ResourceBlockBehavior, ResourceBlockBehavior.Executor> GET_RESOURCE = DataKey.behavior(Identifier.parse("get_resource"), ResourceBlockBehavior.class, ResourceBlockBehavior.Executor.class);

    public static final BehaviorKey<ResourceCountBlockBehavior, ResourceCountBlockBehavior.Executor> GET_RESOURCE_COUNT = DataKey.behavior(Identifier.parse("get_resource_count"), ResourceCountBlockBehavior.class, ResourceCountBlockBehavior.Executor.class);

    public static final BehaviorKey<Float, Float> GET_TRANSLUCENCY = DataKey.behavior(Identifier.parse("get_translucency"), Float.class);

    public static final BehaviorKey<Boolean, Boolean> IS_SOLID = DataKey.behavior(Identifier.parse("is_solid"), Boolean.class);

    public static final BehaviorKey<Boolean, Boolean> IS_LIQUID = DataKey.behavior(Identifier.parse("is_liquid"), Boolean.class);

    public static final BehaviorKey<Boolean, Boolean> USES_WATERLOGGING = DataKey.behavior(Identifier.parse("uses_waterlogging"), Boolean.class);

    public static final BehaviorKey<Boolean, Boolean> IS_TOP_SOLID = DataKey.behavior(Identifier.parse("is_top_solid"), Boolean.class);

    public static final BehaviorKey<Boolean, Boolean> IS_STAIRS = DataKey.behavior(Identifier.parse("is_stairs"), Boolean.class);

    public static final BehaviorKey<Boolean, Boolean> IS_SLAB = DataKey.behavior(Identifier.parse("is_slab"), Boolean.class);

    public static final BehaviorKey<Boolean, Boolean> IS_REPLACEABLE = DataKey.behavior(Identifier.parse("is_replaceable"), Boolean.class);

    public static final BehaviorKey<Boolean, Boolean> IS_SUPER_HOT = DataKey.behavior(Identifier.parse("is_super_hot"), Boolean.class);

    public static final BehaviorKey<Boolean, Boolean> IS_FLAMMABLE = DataKey.behavior(Identifier.parse("is_flammable"), Boolean.class);

    public static final BehaviorKey<ColorBlockBehavior, ColorBlockBehavior.Executor> GET_COLOR = DataKey.behavior(Identifier.parse("get_color"), ColorBlockBehavior.class, ColorBlockBehavior.Executor.class);

    public static final BehaviorKey<Integer, Integer> GET_LIGHT = DataKey.behavior(Identifier.parse("get_light"), Integer.class);

    public static final BehaviorKey<Boolean, Boolean> IS_ALWAYS_DESTROYABLE = DataKey.behavior(Identifier.parse("is_always_destroyable"), Boolean.class);

    public static final BehaviorKey<Integer, Integer> GET_TICK_DELAY = DataKey.behavior(Identifier.parse("get_tick_delay"), Integer.class);

    public static final BehaviorKey<SurviveBlockBehavior, SurviveBlockBehavior.Executor> CAN_SURVIVE = DataKey.behavior(Identifier.parse("can_survive"), SurviveBlockBehavior.class, SurviveBlockBehavior.Executor.class);

    public static final BehaviorKey<ComplexBlockBehavior, ComplexBlockBehavior.Executor> CHECK_ALIVE = DataKey.behavior(Identifier.parse("check_alive"), ComplexBlockBehavior.class, ComplexBlockBehavior.Executor.class);

    public static final BehaviorKey<Boolean, Boolean> IS_FLOODABLE = DataKey.behavior(Identifier.parse("is_floodable"), Boolean.class);

    public static final BehaviorKey<Boolean, Boolean> CAN_INSTATICK = DataKey.behavior(Identifier.parse("is_instaticking"), Boolean.class);

    public static final BehaviorKey<BooleanBlockBehavior, BooleanBlockBehavior.Executor> CAN_SLIDE = DataKey.behavior(Identifier.parse("can_slide"), BooleanBlockBehavior.class, BooleanBlockBehavior.Executor.class);

    public static final BehaviorKey<BooleanBlockBehavior, BooleanBlockBehavior.Executor> IS_FREE_TO_FALL = DataKey.behavior(Identifier.parse("is_free_to_fall"), BooleanBlockBehavior.class, BooleanBlockBehavior.Executor.class);

    public static final BehaviorKey<ComplexBlockBehavior, ComplexBlockBehavior.Executor> START_FALLING = DataKey.behavior(Identifier.parse("start_falling"), ComplexBlockBehavior.class, ComplexBlockBehavior.Executor.class);

    public static final BehaviorKey<FloatBlockBehavior, FloatBlockBehavior.Executor> GET_LIQUID_HEIGHT = DataKey.behavior(Identifier.parse("get_fluid_height"), FloatBlockBehavior.class, FloatBlockBehavior.Executor.class);

    public static final BehaviorKey<Float, Float> GET_RESISTANCE = DataKey.behavior(Identifier.parse("get_resistance"), Float.class);

    public static final BehaviorKey<EntityBlockBehavior, EntityBlockBehavior.Executor> ON_ENTITY_COLLIDE = DataKey.behavior(Identifier.parse("on_entity_collide"), EntityBlockBehavior.class, EntityBlockBehavior.Executor.class);

    public static final BehaviorKey<Integer, Integer> GET_FILTERED_LIGHT = DataKey.behavior(Identifier.parse("get_filtered_light"), Integer.class, Integer.class);

    public static final BehaviorKey<Boolean, Boolean> CAN_DAMAGE_ITEM = DataKey.behavior(Identifier.parse("can_damage_item"), Boolean.class);

    public static final BehaviorKey<MapColorBehavior, MapColorBehavior.Executor> GET_MAP_COLOR = DataKey.behavior(Identifier.parse("get_map_color"), MapColorBehavior.class, MapColorBehavior.Executor.class);

    public static final BehaviorKey<BooleanBlockStateBehavior, BooleanBlockStateBehavior.Executor> CAN_PASS_THROUGH = DataKey.behavior(Identifier.parse("can_pass_through"), BooleanBlockStateBehavior.class, BooleanBlockStateBehavior.Executor.class);

    public static final BehaviorKey<CanBreakBlockBehavior, CanBreakBlockBehavior.Executor> IS_BREAKABLE = DataKey.behavior(Identifier.parse("is_breakable"), CanBreakBlockBehavior.class, CanBreakBlockBehavior.Executor.class);

}
