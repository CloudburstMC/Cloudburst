package org.cloudburstmc.api.block;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.cloudburstmc.api.block.component.*;
import org.cloudburstmc.api.data.ComponentType;

/**
 * Component keys for block placement, interactions, geometry and destruction.
 *
 * <p>Shape queries return block-local geometry.
 */
@SuppressWarnings("rawtypes")
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class BlockComponents {
    /**
     * Handles a player's attack on a block. Returning {@code true} consumes the attack.
     */
    public static final ComponentType<AttackBlockHandler> ATTACK = ComponentType.of("attack", AttackBlockHandler.class);

    /**
     * Resolves the filled bucket for collection, or {@link org.cloudburstmc.api.item.ItemStack#EMPTY} when unavailable.
     */
    public static final ComponentType<BucketPickupHandler> BUCKET_PICKUP = ComponentType.of("bucket_pickup", BucketPickupHandler.class);

    /**
     * Number of ticks a pressed button remains powered before its scheduled release.
     */
    public static final ComponentType<Integer> BUTTON_PRESS_DURATION_TICKS = ComponentType.of("button_press_duration_ticks", Integer.class);

    /**
     * Checks whether placement may replace the existing block with the supplied state.
     */
    public static final ComponentType<ReplaceBlockHandler> CAN_BE_REPLACED = ComponentType.of("can_be_replaced", ReplaceBlockHandler.class);

    /**
     * Checks whether block interaction may call {@link #USE}.
     */
    public static final ComponentType<UseCheckHandler> CAN_BE_USED = ComponentType.of("can_be_used", UseCheckHandler.class);

    /**
     * TODO: Implement block command eligibility checks.
     */
    public static final ComponentType<BooleanBlockHandler> CAN_BE_USED_IN_COMMANDS = ComponentType.of("can_be_used_in_commands", BooleanBlockHandler.class);

    /**
     * Enables random tick selection for this block type. Selected ticks call {@link #ON_RANDOM_TICK}.
     */
    public static final ComponentType<Boolean> CAN_RANDOM_TICK = ComponentType.of("can_random_tick", Boolean.class);

    /**
     * TODO: Implement block sliding checks.
     */
    public static final ComponentType<BooleanBlockHandler> CAN_SLIDE = ComponentType.of("can_slide", BooleanBlockHandler.class);

    /**
     * TODO: Implement spawn support checks.
     */
    public static final ComponentType<BooleanBlockHandler> CAN_SPAWN_ON = ComponentType.of("can_spawn_on", BooleanBlockHandler.class);

    /**
     * Checks whether a block can remain at its position. Placement evaluates this on the proposed block state.
     */
    public static final ComponentType<SurviveBlockHandler> CAN_SURVIVE = ComponentType.of("can_survive", SurviveBlockHandler.class);

    /**
     * TODO: Implement block validity checks.
     */
    public static final ComponentType<ComplexBlockHandler> CHECK_ALIVE = ComponentType.of("check_alive", ComplexBlockHandler.class);

    /**
     * Proposes the changes from fertilization without applying them. An empty result means fertilization is unavailable.
     */
    public static final ComponentType<FertilizeBlockHandler> FERTILIZE = ComponentType.of("fertilize", FertilizeBlockHandler.class);

    /**
     * TODO: Implement block-entity queries.
     */
    public static final ComponentType<GenericBlockHandler> GET_BLOCK_ENTITY = ComponentType.of("get_block_entity", GenericBlockHandler.class);

    /**
     * Resolves geometry for face-support checks, which may differ from collision and outline geometry.
     */
    public static final ComponentType<BlockSupportShapeHandler> GET_BLOCK_SUPPORT_SHAPE = ComponentType.of("get_block_support_shape", BlockSupportShapeHandler.class);

    /**
     * Resolves collision geometry using the supplied block and entity context.
     */
    public static final ComponentType<CollisionShapeHandler> GET_COLLISION_SHAPE = ComponentType.of("get_collision_shape", CollisionShapeHandler.class);

    /**
     * TODO: Implement block-color queries.
     */
    public static final ComponentType<ColorBlockHandler> GET_COLOR = ComponentType.of("get_color", ColorBlockHandler.class);

    /**
     * TODO: Implement block description queries.
     */
    public static final ComponentType<DescriptionBlockHandler> GET_DESCRIPTION_ID = ComponentType.of("get_description_id", DescriptionBlockHandler.class);

    /**
     * Resolves the shape used to test entity overlap before invoking {@link #ON_ENTITY_INSIDE}.
     * Defaults to a full block, independently of the shape that blocks movement.
     */
    public static final ComponentType<VoxelShapeBlockHandler> GET_ENTITY_INSIDE_COLLISION_SHAPE = ComponentType.of("get_entity_inside_collision_shape", VoxelShapeBlockHandler.class);

    /**
     * Resolves experience for a block break using its loot context. Does not spawn experience.
     */
    public static final ComponentType<BlockExperienceHandler> GET_EXPERIENCE = ComponentType.of("get_experience", BlockExperienceHandler.class);

    /**
     * Resolves item drops for explosions using the supplied loot context. Does not spawn items.
     */
    public static final ComponentType<BlockLootHandler> GET_EXPLOSION_LOOT = ComponentType.of("get_explosion_loot", BlockLootHandler.class);

    /**
     * TODO: Implement block gravity queries.
     */
    public static final ComponentType<FloatBlockHandler> GET_GRAVITY = ComponentType.of("get_gravity", FloatBlockHandler.class);

    /**
     * Resolves the state produced by lighting an existing block, without changing the level.
     * Returns {@code null} if the block cannot be lit in its current state.
     */
    public static final ComponentType<IgnitionStateHandler> GET_IGNITED_STATE = ComponentType.of("get_ignited_state", IgnitionStateHandler.class);

    /**
     * Resolves item drops for destruction using the supplied loot context. Does not spawn items.
     */
    public static final ComponentType<BlockLootHandler> GET_LOOT = ComponentType.of("get_loot", BlockLootHandler.class);

    /**
     * Resolves the color used to render this block on a map.
     */
    public static final ComponentType<MapColorHandler> GET_MAP_COLOR = ComponentType.of("get_map_color", MapColorHandler.class);

    /**
     * Resolves selection and outline geometry using the supplied block context.
     */
    public static final ComponentType<BlockShapeHandler> GET_OUTLINE_SHAPE = ComponentType.of("get_outline_shape", BlockShapeHandler.class);

    /**
     * Resolves the picked item, or {@link org.cloudburstmc.api.item.ItemStack#EMPTY} when no item is available.
     */
    public static final ComponentType<PickBlockHandler> GET_PICK_BLOCK = ComponentType.of("get_pick_block", PickBlockHandler.class);

    /**
     * Checks whether the supplied item is allowed to break a block. This is separate from mining speed and loot eligibility.
     */
    public static final ComponentType<CanBreakBlockHandler> IS_BREAKABLE = ComponentType.of("is_breakable", CanBreakBlockHandler.class);

    /**
     * Checks whether a falling block may pass through this block rather than land on it.
     */
    public static final ComponentType<BooleanBlockHandler> IS_FREE_TO_FALL = ComponentType.of("is_free_to_fall", BooleanBlockHandler.class);

    /**
     * Classifies a state as solid support for survival and placement rules that require a solid block.
     * This does not require a full collision cube or a sturdy face.
     */
    public static final ComponentType<BooleanBlockStateHandler> IS_SOLID_SUPPORT = ComponentType.of("is_solid_support", BooleanBlockStateHandler.class);

    /**
     * TODO: Implement block pick eligibility checks.
     */
    public static final ComponentType<BooleanBlockHandler> MAY_PICK = ComponentType.of("may_pick", BooleanBlockHandler.class);

    /**
     * TODO: Implement placement face eligibility checks.
     */
    public static final ComponentType<MayPlaceBlockHandler> MAY_PLACE = ComponentType.of("may_place", MayPlaceBlockHandler.class);

    /**
     * TODO: Implement placement support eligibility checks.
     */
    public static final ComponentType<BooleanBlockHandler> MAY_PLACE_ON = ComponentType.of("may_place_on", BooleanBlockHandler.class);

    /**
     * Removes a destroyed block and applies any multi-block effects. Loot is resolved separately.
     */
    public static final ComponentType<BlockDestroyHandler> ON_DESTROY = ComponentType.of("on_destroy", BlockDestroyHandler.class);

    /**
     * Handles entity collision with a block. Inside-block behavior is provided separately by {@link #ON_ENTITY_INSIDE}.
     */
    public static final ComponentType<EntityBlockHandler> ON_ENTITY_COLLIDE = ComponentType.of("on_entity_collide", EntityBlockHandler.class);

    /**
     * Handles an entity overlapping the block's inside-collision shape.
     */
    public static final ComponentType<EntityInsideBlockHandler> ON_ENTITY_INSIDE = ComponentType.of("on_entity_inside", EntityInsideBlockHandler.class);

    /**
     * Runs before explosion destruction. Returning {@code false} leaves the block intact.
     */
    public static final ComponentType<BlockExplosionHandler> ON_EXPLOSION_HIT = ComponentType.of("on_explosion_hit", BlockExplosionHandler.class);

    /**
     * Handles an entity landing on a block after a fall.
     */
    public static final ComponentType<FallOnBlockHandler> ON_FALL_ON = ComponentType.of("on_fall_on", FallOnBlockHandler.class);

    /**
     * Handles a falling-block entity reaching its landing position, before the landing state is placed.
     */
    public static final ComponentType<FallingLandBlockHandler> ON_FALLING_LAND = ComponentType.of("on_falling_land", FallingLandBlockHandler.class);

    /**
     * TODO: Implement block lightning callbacks.
     */
    public static final ComponentType<ComplexBlockHandler> ON_LIGHTNING_HIT = ComponentType.of("on_lightning_hit", ComplexBlockHandler.class);

    /**
     * Handles a change to a neighboring block.
     */
    public static final ComponentType<NeighborBlockHandler> ON_NEIGHBOUR_CHANGED = ComponentType.of("on_neighbour_changed", NeighborBlockHandler.class);

    /**
     * Applies player placement of the resolved state. Returning {@code false} reports unsuccessful placement.
     */
    public static final ComponentType<PlaceBlockHandler> ON_PLACE = ComponentType.of("on_place", PlaceBlockHandler.class);

    /**
     * Handles a projectile hitting a block.
     */
    public static final ComponentType<EntityBlockHandler> ON_PROJECTILE_HIT = ComponentType.of("on_projectile_hit", EntityBlockHandler.class);

    /**
     * Handles a randomly selected tick for a block type with {@link #CAN_RANDOM_TICK} enabled.
     */
    public static final ComponentType<TickBlockHandler> ON_RANDOM_TICK = ComponentType.of("on_random_tick", TickBlockHandler.class);

    /**
     * Handles a redstone update.
     */
    public static final ComponentType<ComplexBlockHandler> ON_REDSTONE_UPDATE = ComponentType.of("on_redstone_update", ComplexBlockHandler.class);

    /**
     * Handles replacement by a different block type. Receives the old block, after the new state has been stored.
     */
    public static final ComponentType<ComplexBlockHandler> ON_REMOVE = ComponentType.of("on_remove", ComplexBlockHandler.class);

    /**
     * TODO: Implement entity standing callbacks.
     */
    public static final ComponentType<EntityBlockHandler> ON_STAND_ON = ComponentType.of("on_stand_on", EntityBlockHandler.class);

    /**
     * TODO: Implement entity step-off callbacks.
     */
    public static final ComponentType<EntityBlockHandler> ON_STEP_OFF = ComponentType.of("on_step_off", EntityBlockHandler.class);

    /**
     * TODO: Implement entity step-on callbacks.
     */
    public static final ComponentType<EntityBlockHandler> ON_STEP_ON = ComponentType.of("on_step_on", EntityBlockHandler.class);

    /**
     * Handles scheduled or explicitly requested block ticks. The random source may be {@code null}.
     */
    public static final ComponentType<TickBlockHandler> ON_TICK = ComponentType.of("on_tick", TickBlockHandler.class);

    /**
     * Resolves the proposed placement state before survival, collision and placement checks. Does not change the level.
     */
    public static final ComponentType<PlacementStateHandler> RESOLVE_PLACEMENT_STATE = ComponentType.of("resolve_placement_state", PlacementStateHandler.class);

    /**
     * Starts a falling-block operation.
     */
    public static final ComponentType<ComplexBlockHandler> START_FALLING = ComponentType.of("start_falling", ComplexBlockHandler.class);

    /**
     * Classifies states that participate in suffocation collision checks.
     */
    public static final ComponentType<BooleanBlockStateHandler> SUFFOCATING = ComponentType.of("suffocating", BooleanBlockStateHandler.class);

    /**
     * Handles block interaction after {@link #CAN_BE_USED} permits it. Returning {@code true} consumes the interaction.
     */
    public static final ComponentType<UseBlockHandler> USE = ComponentType.of("use", UseBlockHandler.class);
}
