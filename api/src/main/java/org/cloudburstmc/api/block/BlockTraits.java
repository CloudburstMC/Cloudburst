package org.cloudburstmc.api.block;

import com.google.common.collect.ImmutableSet;
import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.block.trait.BooleanBlockTrait;
import org.cloudburstmc.api.block.trait.EnumBlockTrait;
import org.cloudburstmc.api.block.trait.IntegerBlockTrait;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.api.util.data.*;

/**
 * Built-in block state properties.
 *
 * <p>A block type supports only the traits listed by {@link BlockType#getTraits()}.
 * Shared numeric traits may have different meanings or useful stages on different block types.
 */
@UtilityClass
public class BlockTraits {

    /**
     * Mounting arrangement for a bell or grindstone, such as standing, hanging or wall-mounted.
     */
    public static final EnumBlockTrait<AttachmentType> ATTACHMENT = EnumBlockTrait.of("attachment", AttachmentType.class);

    /**
     * Lengthwise axis of a pillar-shaped block.
     */
    public static final EnumBlockTrait<Direction.Axis> AXIS = EnumBlockTrait.of("axis", "pillar_axis", Direction.Axis.class, ImmutableSet.copyOf(Direction.Axis.values()), Direction.Axis.Y);

    /**
     * Size of the leaves on a bamboo segment.
     */
    public static final EnumBlockTrait<BambooLeafSize> BAMBOO_LEAF_SIZE = EnumBlockTrait.of("bamboo_leaf_size", BambooLeafSize.class);

    /**
     * Thickness of a bamboo stalk.
     */
    public static final EnumBlockTrait<BambooStalkThickness> BAMBOO_STALK_THICKNESS = EnumBlockTrait.of("bamboo_stalk_thickness", BambooStalkThickness.class);

    /**
     * Direction in which an amethyst bud or cluster points, away from its support.
     */
    public static final EnumBlockTrait<Direction> BLOCK_FACE = EnumBlockTrait.of("block_face", "minecraft:block_face", Direction.class);

    /**
     * Horizontal orientation restricted to north, south, east and west.
     */
    public static final EnumBlockTrait<CardinalDirection> CARDINAL_DIRECTION = EnumBlockTrait.of("cardinal_direction", "minecraft:cardinal_direction", CardinalDirection.class, ImmutableSet.of(CardinalDirection.SOUTH, CardinalDirection.WEST, CardinalDirection.NORTH, CardinalDirection.EAST), CardinalDirection.SOUTH);

    /**
     * Kind of liquid or powder snow held by a cauldron. The amount is stored in {@link #FILL_LEVEL}.
     */
    public static final EnumBlockTrait<Bucket> CAULDRON_TYPE = EnumBlockTrait.of("cauldron_type", "cauldron_liquid", Bucket.class, Bucket.WATER, Bucket.LAVA, Bucket.POWDER_SNOW);

    /**
     * Outward-facing horizontal direction of a wall coral fan.
     */
    public static final EnumBlockTrait<Direction> CORAL_DIRECTION = EnumBlockTrait.of("coral_direction", Direction.class, Direction.WEST, Direction.EAST, Direction.NORTH, Direction.SOUTH);

    /**
     * Cracking stage of a turtle or sniffer egg.
     */
    public static final EnumBlockTrait<CrackedState> CRACKED_STATE = EnumBlockTrait.of("cracked_state", CrackedState.class);

    /**
     * Front and top directions of a crafter.
     */
    public static final EnumBlockTrait<CrafterOrientation> CRAFTER_ORIENTATION = EnumBlockTrait.of("orientation", CrafterOrientation.class);

    /**
     * Whether a creaking heart is uprooted, dormant or awake.
     */
    public static final EnumBlockTrait<CreakingHeartState> CREAKING_HEART_STATE = EnumBlockTrait.of("creaking_heart_state", CreakingHeartState.class);

    /**
     * Horizontal orientation using {@link Direction}. Its meaning depends on the block type.
     */
    public static final EnumBlockTrait<Direction> DIRECTION = EnumBlockTrait.of("direction", Direction.class, Direction.EAST, Direction.SOUTH, Direction.WEST, Direction.NORTH);

    /**
     * Tilt stage of a big dripleaf's leaf.
     */
    public static final EnumBlockTrait<DripleafTilt> DRIPLEAF_TILT = EnumBlockTrait.of("dripleaf_tilt", "big_dripleaf_tilt", DripleafTilt.class);

    /**
     * Shape of a pointed dripstone or sulfur spike segment, including tips and merged tips.
     */
    public static final EnumBlockTrait<DripstoneThickness> DRIPSTONE_THICKNESS = EnumBlockTrait.of("dripstone_thickness", DripstoneThickness.class);

    /**
     * Orientation supporting all six directions. Its meaning depends on the block type.
     */
    public static final EnumBlockTrait<Direction> FACING_DIRECTION = EnumBlockTrait.of("facing_direction", Direction.class, Direction.DOWN, Direction.EAST, Direction.WEST, Direction.SOUTH, Direction.NORTH, Direction.UP);

    /**
     * Mounting face and handle alignment of a lever.
     */
    public static final EnumBlockTrait<LeverDirection> LEVER_DIRECTION = EnumBlockTrait.of("lever_direction", LeverDirection.class);

    /**
     * Height of the pale moss carpet's eastern side.
     */
    public static final EnumBlockTrait<WallConnectionType> PALE_MOSS_CARPET_SIDE_EAST = EnumBlockTrait.of("pale_moss_carpet_side_east", WallConnectionType.class);

    /**
     * Height of the pale moss carpet's northern side.
     */
    public static final EnumBlockTrait<WallConnectionType> PALE_MOSS_CARPET_SIDE_NORTH = EnumBlockTrait.of("pale_moss_carpet_side_north", WallConnectionType.class);

    /**
     * Height of the pale moss carpet's southern side.
     */
    public static final EnumBlockTrait<WallConnectionType> PALE_MOSS_CARPET_SIDE_SOUTH = EnumBlockTrait.of("pale_moss_carpet_side_south", WallConnectionType.class);

    /**
     * Height of the pale moss carpet's western side.
     */
    public static final EnumBlockTrait<WallConnectionType> PALE_MOSS_CARPET_SIDE_WEST = EnumBlockTrait.of("pale_moss_carpet_side_west", WallConnectionType.class);

    /**
     * Axis along a Nether portal's width. {@code X} and {@code Z} select a vertical plane.
     * {@code Y} represents an unspecified axis.
     */
    public static final EnumBlockTrait<Direction.Axis> PORTAL_AXIS = EnumBlockTrait.of("portal_axis", Direction.Axis.class, Direction.Axis.Y, Direction.Axis.X, Direction.Axis.Z);

    /**
     * Moisture and eruption state of potent sulfur.
     */
    public static final EnumBlockTrait<PotentSulfurState> POTENT_SULFUR_STATE = EnumBlockTrait.of("potent_sulfur_state", PotentSulfurState.class);

    /**
     * Shape of a regular rail, including straight, ascending and curved connections.
     */
    public static final EnumBlockTrait<RailDirection> RAIL_DIRECTION = EnumBlockTrait.of("rail_direction", RailDirection.class);

    /**
     * Whether seagrass is a single plant or the upper or lower half of a tall plant.
     */
    public static final EnumBlockTrait<SeaGrassType> SEA_GRASS_TYPE = EnumBlockTrait.of("sea_grass_type", SeaGrassType.class);

    /**
     * Rotation of a standing or ceiling-attached sign in sixteen horizontal directions.
     */
    public static final EnumBlockTrait<CardinalDirection> SIGN_DIRECTION = EnumBlockTrait.of("sign_direction", "ground_sign_direction", CardinalDirection.class);

    /**
     * Shape of a powered, detector or activator rail. Curved connections are not supported.
     */
    public static final EnumBlockTrait<RailDirection> SIMPLE_RAIL_DIRECTION = EnumBlockTrait.of("simple_rail_direction", "rail_direction", RailDirection.class, ImmutableSet.copyOf(RailDirection.simpleValues()), RailDirection.NORTH_SOUTH);

    /**
     * Whether a slab occupies the top or bottom half of its block space.
     */
    public static final EnumBlockTrait<SlabSlot> SLAB_SLOT = EnumBlockTrait.of("slab_slot", SlabSlot.class);

    /**
     * Straight, inner-corner or outer-corner shape of a stair.
     */
    public static final EnumBlockTrait<StairShape> STAIR_SHAPE = EnumBlockTrait.of("stair_shape", "minecraft:corner", StairShape.class);

    /**
     * Operating mode of a structure block.
     */
    public static final EnumBlockTrait<StructureBlockType> STRUCTURE_BLOCK_TYPE = EnumBlockTrait.of("structure_block_type", StructureBlockType.class);

    /**
     * Torch orientation. {@code UP} is floor-mounted, horizontal values are wall-mounted,
     * and {@code DOWN} represents an unspecified orientation.
     */
    public static final EnumBlockTrait<Direction> TORCH_DIRECTION = EnumBlockTrait.of("torch_direction", "torch_facing_direction", Direction.class, Direction.DOWN, Direction.WEST, Direction.EAST, Direction.NORTH, Direction.SOUTH, Direction.UP);

    /**
     * Number of turtle eggs occupying the block, from one to four.
     */
    public static final EnumBlockTrait<TurtleEggCount> TURTLE_EGG_COUNT = EnumBlockTrait.of("turtle_egg_count", TurtleEggCount.class);

    /**
     * Activation, unlocking or reward-ejection stage of a vault.
     */
    public static final EnumBlockTrait<VaultState> VAULT_STATE = EnumBlockTrait.of("vault_state", VaultState.class);

    /**
     * Height of a wall's eastern connection, or no connection.
     */
    public static final EnumBlockTrait<WallConnectionType> WALL_CONNECTION_EAST = EnumBlockTrait.of("wall_connection_east", "wall_connection_type_east", WallConnectionType.class);

    /**
     * Height of a wall's northern connection, or no connection.
     */
    public static final EnumBlockTrait<WallConnectionType> WALL_CONNECTION_NORTH = EnumBlockTrait.of("wall_connection_north", "wall_connection_type_north", WallConnectionType.class);

    /**
     * Height of a wall's southern connection, or no connection.
     */
    public static final EnumBlockTrait<WallConnectionType> WALL_CONNECTION_SOUTH = EnumBlockTrait.of("wall_connection_south", "wall_connection_type_south", WallConnectionType.class);

    /**
     * Height of a wall's western connection, or no connection.
     */
    public static final EnumBlockTrait<WallConnectionType> WALL_CONNECTION_WEST = EnumBlockTrait.of("wall_connection_west", "wall_connection_type_west", WallConnectionType.class);


    /**
     * Age or progression value from {@code 0} to {@code 15}. The meaningful stages depend on the block type.
     */
    public static final IntegerBlockTrait AGE = IntegerBlockTrait.from("age", 16);

    /**
     * Number of cake slices already eaten, from {@code 0} to {@code 6}.
     */
    public static final IntegerBlockTrait BITE_COUNTER = IntegerBlockTrait.from("bite_counter", 7);

    /**
     * Occupied slots in a chiseled bookshelf as a six-bit mask, not a book count.
     * Bit {@code 1 << slot} marks an occupied slot, numbered left to right across the top row, then the bottom row.
     */
    public static final IntegerBlockTrait BOOKS_STORED = IntegerBlockTrait.from("books_stored", 64);

    /**
     * Visible brushing progress of suspicious sand or gravel, from {@code 0} to {@code 3}.
     */
    public static final IntegerBlockTrait BRUSHED_PROGRESS = IntegerBlockTrait.from("brushed_progress", 4);

    /**
     * Number of candles minus one. Values {@code 0} through {@code 3} represent one through four candles.
     */
    public static final IntegerBlockTrait CANDLES = IntegerBlockTrait.from("candles", 0, 3);

    /**
     * Growth age of a cave vine segment, from {@code 0} to {@code 25}.
     */
    public static final IntegerBlockTrait CAVE_VINE_AGE = IntegerBlockTrait.from("growing_plant_age", 26);

    /**
     * Rotation index of a chalkboard, from {@code 0} to {@code 15}.
     */
    public static final IntegerBlockTrait CHALKBOARD_DIRECTION = IntegerBlockTrait.from("chalkboard_direction", "direction", 0, 15, 0);

    /**
     * Horizontal orientation index of an Education chemistry table, from {@code 0} to {@code 3}.
     */
    public static final IntegerBlockTrait CHEMISTRY_TABLE_DIRECTION = IntegerBlockTrait.from("chemistry_table_direction", "direction", 0, 3, 0);

    /**
     * Number of sea pickles minus one. Values {@code 0} through {@code 3} represent one through four pickles.
     */
    public static final IntegerBlockTrait CLUSTER_COUNT = IntegerBlockTrait.from("cluster_count", 4);

    /**
     * Composter stage. {@code 0} is empty, {@code 7} is full but not ready,
     * and {@code 8} is ready to yield bone meal.
     */
    public static final IntegerBlockTrait COMPOSTER_FILL_LEVEL = IntegerBlockTrait.from("composter_fill_level", 9);

    /**
     * One of the two visual orientations of a floor coral fan. This is not a wall attachment direction.
     */
    public static final IntegerBlockTrait CORAL_FAN_DIRECTION = IntegerBlockTrait.from("coral_fan_direction", 2);

    /**
     * Variant discriminator on bone and hay blocks, from {@code 0} to {@code 3}.
     */
    public static final IntegerBlockTrait DEPRECATED = IntegerBlockTrait.from("deprecated", 4);

    /**
     * Cauldron fill level, from {@code 0} for empty to {@code 6} for full.
     */
    public static final IntegerBlockTrait FILL_LEVEL = IntegerBlockTrait.from("fill_level", 7);

    /**
     * Growth or coverage stage from {@code 0} to {@code 7}. The meaningful stages depend on the block type.
     */
    public static final IntegerBlockTrait GROWTH = IntegerBlockTrait.from("growth", 8);

    /**
     * Number of snow layers minus one. Values {@code 0} through {@code 7} represent one through eight layers.
     */
    public static final IntegerBlockTrait HEIGHT = IntegerBlockTrait.from("height", 8);

    /**
     * Stored honey level of a hive or bee nest, from {@code 0} to {@code 5}.
     */
    public static final IntegerBlockTrait HONEY_LEVEL = IntegerBlockTrait.from("honey_level", 6);

    /**
     * Surface-texture variant of a huge mushroom block, from {@code 0} to {@code 15}.
     * Despite its name, this is a variant index rather than a face bitmask.
     */
    public static final IntegerBlockTrait HUGE_MUSHROOM_BITS = IntegerBlockTrait.from("huge_mushroom_bits", 16);

    /**
     * Rotation index around a jigsaw block's facing direction, from {@code 0} to {@code 3}.
     */
    public static final IntegerBlockTrait JIGSAW_ROTATION = IntegerBlockTrait.from("rotation", 4);

    /**
     * Growth age of kelp, from {@code 0} to {@code 25}. Natural tip growth stops at {@code 25},
     * but bone meal can still extend the plant. Non-tip segments also use {@code 25}.
     */
    public static final IntegerBlockTrait KELP_AGE = IntegerBlockTrait.from("kelp_age", 26);

    /**
     * Liquid level and falling flag, from {@code 0} to {@code 15}.
     * {@code 0} is a source, the lower three bits describe flow depth, and bit {@code 8} marks falling liquid.
     * Use {@link LiquidState} for liquid queries.
     */
    public static final IntegerBlockTrait LIQUID_DEPTH = IntegerBlockTrait.from("liquid_depth", 16);

    /**
     * Farmland moisture, from {@code 0} for dry to {@code 7} for fully hydrated.
     */
    public static final IntegerBlockTrait MOISTURIZED_AMOUNT = IntegerBlockTrait.from("moisturized_amount", 8);

    /**
     * Occupied faces of glow lichen, sculk vein or resin clumps as a six-bit mask.
     * Bits {@code 1}, {@code 2}, {@code 4}, {@code 8}, {@code 16} and {@code 32}
     * represent down, up, south, west, north and east respectively.
     */
    public static final IntegerBlockTrait MULTI_FACE_DIRECTION = IntegerBlockTrait.from("multi_face_direction", "multi_face_direction_bits", 0, 63, 0);

    /**
     * Position in a powered shelf chain. {@code 0} is unconnected, {@code 1} is the right end,
     * {@code 2} is the center, and {@code 3} is the left end.
     */
    public static final IntegerBlockTrait POWERED_SHELF_TYPE = IntegerBlockTrait.from("powered_shelf_type", 4);

    /**
     * Growth stage of a hanging mangrove propagule, from {@code 0} to {@code 4}.
     */
    public static final IntegerBlockTrait PROPAGULE_STAGE = IntegerBlockTrait.from("propagule_stage", 5);

    /**
     * Redstone signal strength, from {@code 0} to {@code 15}.
     */
    public static final IntegerBlockTrait REDSTONE_SIGNAL = IntegerBlockTrait.from("redstone_signal", 16);

    /**
     * Rehydration stage of a dried ghast, from {@code 0} to {@code 3}.
     */
    public static final IntegerBlockTrait REHYDRATION_LEVEL = IntegerBlockTrait.from("rehydration_level", 4);

    /**
     * Repeater delay minus one. Values {@code 0} through {@code 3} represent one through four redstone ticks.
     */
    public static final IntegerBlockTrait REPEATER_DELAY = IntegerBlockTrait.from("repeater_delay", 4);

    /**
     * Number of stored respawn anchor charges, from {@code 0} to {@code 4}.
     */
    public static final IntegerBlockTrait RESPAWN_ANCHOR_CHARGE = IntegerBlockTrait.from("respawn_anchor_charge", 5);

    /**
     * Sculk sensor phase. {@code 0} is inactive, {@code 1} is active, and {@code 2} is cooldown.
     */
    public static final IntegerBlockTrait SCULK_SENSOR_PHASE = IntegerBlockTrait.from("sculk_sensor_phase", 3);

    /**
     * Growth stage of a shelf mushroom, from {@code 0} to {@code 1}.
     */
    public static final IntegerBlockTrait SHELF_MUSHROOM_GROWTH = IntegerBlockTrait.from("shelf_mushroom_growth", "growth", 0, 1, 0);

    /**
     * Scaffolding distance from support, from {@code 0} to {@code 7}.
     * {@code 0} is directly supported and {@code 7} is unsupported.
     */
    public static final IntegerBlockTrait STABILITY = IntegerBlockTrait.from("stability", 8);

    /**
     * Trial spawner stage, from {@code 0} to {@code 5}.
     */
    public static final IntegerBlockTrait TRIAL_SPAWNER_STATE = IntegerBlockTrait.from("trial_spawner_state", 6);

    /**
     * Growth age of twisting vines, from {@code 0} to {@code 25}.
     */
    public static final IntegerBlockTrait TWISTING_VINES_AGE = IntegerBlockTrait.from("twisting_vines_age", 26);

    /**
     * Vine attachments as a horizontal face mask. Bits {@code 1}, {@code 2}, {@code 4} and {@code 8}
     * represent south, west, north and east respectively.
     */
    public static final IntegerBlockTrait VINE_DIRECTION_BITS = IntegerBlockTrait.from("vine_direction_bits", 16);

    /**
     * Growth age of weeping vines, from {@code 0} to {@code 25}.
     */
    public static final IntegerBlockTrait WEEPING_VINES_AGE = IntegerBlockTrait.from("weeping_vines_age", 26);


    /**
     * Whether a sculk shrieker is capable of summoning a warden.
     */
    public static final BooleanBlockTrait CAN_SUMMON = BooleanBlockTrait.of("can_summon");

    /**
     * Whether a fence, pane, bar or tripwire connects to its eastern neighbor.
     */
    public static final BooleanBlockTrait CONNECTION_EAST = BooleanBlockTrait.of("connection_east", "minecraft:connection_east");

    /**
     * Whether a fence, pane, bar or tripwire connects to its northern neighbor.
     */
    public static final BooleanBlockTrait CONNECTION_NORTH = BooleanBlockTrait.of("connection_north", "minecraft:connection_north");

    /**
     * Whether a fence, pane, bar or tripwire connects to its southern neighbor.
     */
    public static final BooleanBlockTrait CONNECTION_SOUTH = BooleanBlockTrait.of("connection_south", "minecraft:connection_south");

    /**
     * Whether a fence, pane, bar or tripwire connects to its western neighbor.
     */
    public static final BooleanBlockTrait CONNECTION_WEST = BooleanBlockTrait.of("connection_west", "minecraft:connection_west");

    /**
     * Whether a crafter is in its crafting animation.
     */
    public static final BooleanBlockTrait CRAFTER_CRAFTING = BooleanBlockTrait.of("crafter_crafting", "crafting");

    /**
     * Whether a crafter is triggered by redstone power.
     */
    public static final BooleanBlockTrait CRAFTER_TRIGGERED = BooleanBlockTrait.of("crafter_triggered", "triggered_bit");

    /**
     * Whether a big dripleaf segment carries the leaf rather than only the stem.
     */
    public static final BooleanBlockTrait DRIPLEAF_HEAD = BooleanBlockTrait.of("dripleaf_head", "big_dripleaf_head");

    /**
     * TNT explosion flag, separate from a primed TNT entity's fuse.
     */
    public static final BooleanBlockTrait EXPLODE = BooleanBlockTrait.of("explode", "explode_bit");

    /**
     * Whether a sapling or bamboo has reached its second age stage.
     */
    public static final BooleanBlockTrait HAS_AGE = BooleanBlockTrait.of("has_age", "age_bit");

    /**
     * Whether a bubble column pulls entities down rather than pushing them up.
     */
    public static final BooleanBlockTrait HAS_DRAG_DOWN = BooleanBlockTrait.of("drag_down");

    /**
     * Whether an End portal frame contains an eye of ender.
     */
    public static final BooleanBlockTrait HAS_END_PORTAL_EYE = BooleanBlockTrait.of("has_end_portal_eye", "end_portal_eye_bit");

    /**
     * Whether bedrock supports fire that burns indefinitely.
     */
    public static final BooleanBlockTrait HAS_INFINIBURN = BooleanBlockTrait.of("has_infiniburn", "infiniburn_bit");

    /**
     * Whether an item frame displays a map.
     */
    public static final BooleanBlockTrait HAS_MAP = BooleanBlockTrait.of("has_map", "item_frame_map_bit");

    /**
     * Whether an item frame displays a photo.
     */
    public static final BooleanBlockTrait HAS_PHOTO = BooleanBlockTrait.of("has_photo", "item_frame_photo_bit");

    /**
     * Whether a wall has its central post.
     */
    public static final BooleanBlockTrait HAS_POST = BooleanBlockTrait.of("has_post", "wall_post_bit");

    /**
     * Whether scaffolding is marked for a stability check.
     */
    public static final BooleanBlockTrait HAS_STABILITY_CHECK = BooleanBlockTrait.of("has_stability_check", "stability_check");

    /**
     * Block-specific update flag. On leaves it marks a decay check, and on flower pots it marks plant contents.
     */
    public static final BooleanBlockTrait HAS_UPDATE = BooleanBlockTrait.of("has_update", "update_bit");

    /**
     * Whether a sculk shrieker is currently shrieking.
     */
    public static final BooleanBlockTrait IS_ACTIVE = BooleanBlockTrait.of("is_active", "active");

    /**
     * Whether tripwire is attached to a complete hook circuit, or a hanging sign uses its attached mounting form.
     */
    public static final BooleanBlockTrait IS_ATTACHED = BooleanBlockTrait.of("is_attached", "attached_bit");

    /**
     * Whether a sculk catalyst is blooming.
     */
    public static final BooleanBlockTrait IS_BLOOMING = BooleanBlockTrait.of("is_blooming", "bloom");

    /**
     * Whether the first brewing stand bottle slot is occupied. This does not indicate brewing progress.
     */
    public static final BooleanBlockTrait IS_BREWING_A = BooleanBlockTrait.of("is_brewing_a", "brewing_stand_slot_a_bit");

    /**
     * Whether the second brewing stand bottle slot is occupied. This does not indicate brewing progress.
     */
    public static final BooleanBlockTrait IS_BREWING_B = BooleanBlockTrait.of("is_brewing_b", "brewing_stand_slot_b_bit");

    /**
     * Whether the third brewing stand bottle slot is occupied. This does not indicate brewing progress.
     */
    public static final BooleanBlockTrait IS_BREWING_C = BooleanBlockTrait.of("is_brewing_c", "brewing_stand_slot_c_bit");

    /**
     * Whether a button is pressed.
     */
    public static final BooleanBlockTrait IS_BUTTON_PRESSED = BooleanBlockTrait.of("is_button_pressed", "button_pressed_bit");

    /**
     * Whether a command block requires the preceding command block to succeed.
     */
    public static final BooleanBlockTrait IS_CONDITIONAL = BooleanBlockTrait.of("is_conditional", "conditional_bit");

    /**
     * Whether snow covers grass without another block sharing the snow's space.
     */
    public static final BooleanBlockTrait IS_COVERED = BooleanBlockTrait.of("is_covered", "covered_bit");

    /**
     * Whether sea pickles are dry and do not emit light. This does not select a dead coral variant.
     */
    public static final BooleanBlockTrait IS_DEAD = BooleanBlockTrait.of("is_dead", "dead_bit");

    /**
     * Whether tripwire is disarmed and will not trigger its hooks when broken.
     */
    public static final BooleanBlockTrait IS_DISARMED = BooleanBlockTrait.of("is_disarmed", "disarmed_bit");

    /**
     * Whether a door's hinge is on the right rather than the left.
     */
    public static final BooleanBlockTrait IS_DOOR_HINGE = BooleanBlockTrait.of("is_door_hinge", "door_hinge_bit", true);

    /**
     * Whether a campfire is extinguished rather than lit.
     */
    public static final BooleanBlockTrait IS_EXTINGUISHED = BooleanBlockTrait.of("is_extinguished", "extinguished");

    /**
     * Whether a block uses its hanging form. Pointed dripstone uses this for a downward-growing stalactite.
     */
    public static final BooleanBlockTrait IS_HANGING = BooleanBlockTrait.of("is_hanging", "hanging");

    /**
     * Whether a bed block is the head rather than the foot.
     */
    public static final BooleanBlockTrait IS_HEAD_PIECE = BooleanBlockTrait.of("is_head_piece", "head_piece_bit");

    /**
     * Whether a fence gate is lowered to align with neighboring walls.
     */
    public static final BooleanBlockTrait IS_IN_WALL = BooleanBlockTrait.of("is_in_wall", "in_wall_bit");

    /**
     * Whether a candle, candle cake or copper bulb is lit.
     */
    public static final BooleanBlockTrait IS_LIT = BooleanBlockTrait.of("is_lit", "lit");

    /**
     * Whether a bed is occupied.
     */
    public static final BooleanBlockTrait IS_OCCUPIED = BooleanBlockTrait.of("is_occupied", "occupied_bit");

    /**
     * Whether a trial spawner or vault is in its ominous form.
     */
    public static final BooleanBlockTrait IS_OMINOUS = BooleanBlockTrait.of("is_ominous", "ominous");

    /**
     * Whether a door, trapdoor, fence gate or barrel is open, or a lever is switched on.
     */
    public static final BooleanBlockTrait IS_OPEN = BooleanBlockTrait.of("is_open", "open_bit");

    /**
     * Whether a comparator's output indicator is lit.
     */
    public static final BooleanBlockTrait IS_OUTPUT_LIT = BooleanBlockTrait.of("is_output_lit", "output_lit_bit");

    /**
     * Whether a comparator uses subtraction rather than comparison mode.
     */
    public static final BooleanBlockTrait IS_OUTPUT_SUBTRACT = BooleanBlockTrait.of("is_output_subtract", "output_subtract_bit");

    /**
     * Whether leaves are persistent rather than subject to natural decay.
     */
    public static final BooleanBlockTrait IS_PERSISTENT = BooleanBlockTrait.of("is_persistent", "persistent_bit");

    /**
     * Block-specific redstone-powered state.
     */
    public static final BooleanBlockTrait IS_POWERED = BooleanBlockTrait.of("is_powered", "powered_bit");

    /**
     * Whether tripwire is suspended without a supporting block underneath.
     */
    public static final BooleanBlockTrait IS_SUSPENDED = BooleanBlockTrait.of("is_suspended", "suspended_bit");

    /**
     * Block-specific toggle state. On a hopper, {@code true} disables item transfer.
     */
    public static final BooleanBlockTrait IS_TOGGLED = BooleanBlockTrait.of("is_toggled", "toggle_bit");

    /**
     * Whether a dispenser or dropper has been triggered by redstone power.
     */
    public static final BooleanBlockTrait IS_TRIGGERED = BooleanBlockTrait.of("is_triggered", "triggered_bit");

    /**
     * Whether a block is the upper half of a two-block plant or door, or the upper form of pale moss carpet.
     */
    public static final BooleanBlockTrait IS_UPPER_BLOCK = BooleanBlockTrait.of("is_upper_block", "upper_block_bit");

    /**
     * Whether stairs or a trapdoor occupy the upper half of the block space.
     */
    public static final BooleanBlockTrait IS_UPSIDE_DOWN = BooleanBlockTrait.of("is_upside_down", "upside_down_bit");

    /**
     * Whether a creaking heart was naturally generated.
     */
    public static final BooleanBlockTrait NATURAL = BooleanBlockTrait.of("natural");

    /**
     * Whether a pale hanging moss segment is the bottom tip.
     */
    public static final BooleanBlockTrait TIP = BooleanBlockTrait.of("tip");
}
