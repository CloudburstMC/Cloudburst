package org.cloudburstmc.api.block;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.cloudburstmc.api.block.component.*;
import org.cloudburstmc.api.data.ComponentType;

@SuppressWarnings("rawtypes")
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class BlockComponents {
    public static final ComponentType<Integer> BUTTON_PRESS_DURATION_TICKS = ComponentType.of("button_press_duration_ticks", Integer.class);
    public static final ComponentType<ReplaceBlockHandler> CAN_BE_REPLACED = ComponentType.of("can_be_replaced", ReplaceBlockHandler.class);
    public static final ComponentType<BooleanBlockHandler> CAN_BE_SILK_TOUCHED = ComponentType.of("can_be_silk_touched", BooleanBlockHandler.class);
    public static final ComponentType<UseCheckHandler> CAN_BE_USED = ComponentType.of("can_be_used", UseCheckHandler.class);
    public static final ComponentType<BooleanBlockHandler> CAN_BE_USED_IN_COMMANDS = ComponentType.of("can_be_used_in_commands", BooleanBlockHandler.class);
    public static final ComponentType<Boolean> CAN_DAMAGE_ITEM = ComponentType.of("can_damage_item", Boolean.class);
    public static final ComponentType<Boolean> CAN_RANDOM_TICK = ComponentType.of("can_random_tick", Boolean.class);
    public static final ComponentType<BooleanBlockHandler> CAN_SLIDE = ComponentType.of("can_slide", BooleanBlockHandler.class);
    public static final ComponentType<BooleanBlockHandler> CAN_SPAWN_ON = ComponentType.of("can_spawn_on", BooleanBlockHandler.class);
    public static final ComponentType<SurviveBlockHandler> CAN_SURVIVE = ComponentType.of("can_survive", SurviveBlockHandler.class);
    public static final ComponentType<ComplexBlockHandler> CHECK_ALIVE = ComponentType.of("check_alive", ComplexBlockHandler.class);
    public static final ComponentType<DropResourceBlockHandler> DROP_RESOURCE = ComponentType.of("drop_resource", DropResourceBlockHandler.class);
    public static final ComponentType<GenericBlockHandler> GET_BLOCK_ENTITY = ComponentType.of("get_block_entity", GenericBlockHandler.class);
    public static final ComponentType<BlockSupportShapeHandler> GET_BLOCK_SUPPORT_SHAPE = ComponentType.of("get_block_support_shape", BlockSupportShapeHandler.class);
    public static final ComponentType<CollisionShapeHandler> GET_COLLISION_SHAPE = ComponentType.of("get_collision_shape", CollisionShapeHandler.class);
    public static final ComponentType<ColorBlockHandler> GET_COLOR = ComponentType.of("get_color", ColorBlockHandler.class);
    public static final ComponentType<DescriptionBlockHandler> GET_DESCRIPTION_ID = ComponentType.of("get_description_id", DescriptionBlockHandler.class);
    public static final ComponentType<FloatBlockHandler> GET_DESTROY_SPEED = ComponentType.of("get_destroy_speed", FloatBlockHandler.class);
    public static final ComponentType<VoxelShapeBlockHandler> GET_ENTITY_INSIDE_COLLISION_SHAPE = ComponentType.of("get_entity_inside_collision_shape", VoxelShapeBlockHandler.class);
    public static final ComponentType<ExpBlockHandler> GET_EXPERIENCE_DROP = ComponentType.of("get_experience_drop", ExpBlockHandler.class);
    public static final ComponentType<FloatBlockHandler> GET_GRAVITY = ComponentType.of("get_gravity", FloatBlockHandler.class);
    public static final ComponentType<MapColorHandler> GET_MAP_COLOR = ComponentType.of("get_map_color", MapColorHandler.class);
    public static final ComponentType<BlockShapeHandler> GET_OUTLINE_SHAPE = ComponentType.of("get_outline_shape", BlockShapeHandler.class);
    public static final ComponentType<PickBlockHandler> GET_PICK_BLOCK = ComponentType.of("get_pick_block", PickBlockHandler.class);
    public static final ComponentType<ResourceBlockHandler> GET_RESOURCE = ComponentType.of("get_resource", ResourceBlockHandler.class);
    public static final ComponentType<ResourceCountBlockHandler> GET_RESOURCE_COUNT = ComponentType.of("get_resource_count", ResourceCountBlockHandler.class);
    public static final ComponentType<ResourceBlockHandler> GET_SILK_TOUCH_RESOURCE = ComponentType.of("get_silk_touch_resource", ResourceBlockHandler.class);
    public static final ComponentType<CanBreakBlockHandler> IS_BREAKABLE = ComponentType.of("is_breakable", CanBreakBlockHandler.class);
    public static final ComponentType<BooleanBlockHandler> IS_FREE_TO_FALL = ComponentType.of("is_free_to_fall", BooleanBlockHandler.class);
    public static final ComponentType<BooleanBlockHandler> MAY_PICK = ComponentType.of("may_pick", BooleanBlockHandler.class);
    public static final ComponentType<MayPlaceBlockHandler> MAY_PLACE = ComponentType.of("may_place", MayPlaceBlockHandler.class);
    public static final ComponentType<BooleanBlockHandler> MAY_PLACE_ON = ComponentType.of("may_place_on", BooleanBlockHandler.class);
    public static final ComponentType<PlayerBlockHandler> ON_DESTROY = ComponentType.of("on_destroy", PlayerBlockHandler.class);
    public static final ComponentType<EntityBlockHandler> ON_ENTITY_COLLIDE = ComponentType.of("on_entity_collide", EntityBlockHandler.class);
    public static final ComponentType<EntityInsideBlockHandler> ON_ENTITY_INSIDE = ComponentType.of("on_entity_inside", EntityInsideBlockHandler.class);
    public static final ComponentType<FallOnBlockHandler> ON_FALL_ON = ComponentType.of("on_fall_on", FallOnBlockHandler.class);
    public static final ComponentType<ComplexBlockHandler> ON_LIGHTNING_HIT = ComponentType.of("on_lightning_hit", ComplexBlockHandler.class);
    public static final ComponentType<NeighborBlockHandler> ON_NEIGHBOUR_CHANGED = ComponentType.of("on_neighbour_changed", NeighborBlockHandler.class);
    public static final ComponentType<PlaceBlockHandler> ON_PLACE = ComponentType.of("on_place", PlaceBlockHandler.class);
    public static final ComponentType<EntityBlockHandler> ON_PROJECTILE_HIT = ComponentType.of("on_projectile_hit", EntityBlockHandler.class);
    public static final ComponentType<TickBlockHandler> ON_RANDOM_TICK = ComponentType.of("on_random_tick", TickBlockHandler.class);
    public static final ComponentType<ComplexBlockHandler> ON_REDSTONE_UPDATE = ComponentType.of("on_redstone_update", ComplexBlockHandler.class);
    public static final ComponentType<ComplexBlockHandler> ON_REMOVE = ComponentType.of("on_remove", ComplexBlockHandler.class);
    public static final ComponentType<EntityBlockHandler> ON_STAND_ON = ComponentType.of("on_stand_on", EntityBlockHandler.class);
    public static final ComponentType<EntityBlockHandler> ON_STEP_OFF = ComponentType.of("on_step_off", EntityBlockHandler.class);
    public static final ComponentType<EntityBlockHandler> ON_STEP_ON = ComponentType.of("on_step_on", EntityBlockHandler.class);
    public static final ComponentType<TickBlockHandler> ON_TICK = ComponentType.of("on_tick", TickBlockHandler.class);
    public static final ComponentType<PlayerBlockHandler> POST_DESTROY = ComponentType.of("post_destroy", PlayerBlockHandler.class);
    public static final ComponentType<SpawnResourcesBlockHandler> SPAWN_RESOURCES = ComponentType.of("spawn_resources", SpawnResourcesBlockHandler.class);
    public static final ComponentType<ComplexBlockHandler> START_FALLING = ComponentType.of("start_falling", ComplexBlockHandler.class);
    public static final ComponentType<BooleanBlockStateHandler> SUFFOCATING = ComponentType.of("suffocating", BooleanBlockStateHandler.class);
    public static final ComponentType<UseBlockHandler> USE = ComponentType.of("use", UseBlockHandler.class);
}
