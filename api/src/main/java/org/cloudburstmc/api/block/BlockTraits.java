package org.cloudburstmc.api.block;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.block.trait.BooleanBlockTrait;
import org.cloudburstmc.api.block.trait.EnumBlockTrait;
import org.cloudburstmc.api.block.trait.IntegerBlockTrait;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.api.util.data.*;

import java.lang.reflect.Array;
import java.util.Arrays;

@UtilityClass
public class BlockTraits {

    public static final EnumBlockTrait<Direction.Axis> AXIS = EnumBlockTrait.of("axis", "pillar_axis", Direction.Axis.class, ImmutableSet.copyOf(Direction.Axis.values()), Direction.Axis.Y);
    public static final EnumBlockTrait<Direction> BLOCK_FACE = EnumBlockTrait.of("block_face", "minecraft:block_face", Direction.class);
    public static final EnumBlockTrait<CardinalDirection> CARDINAL_DIRECTION = EnumBlockTrait.of("cardinal_direction", "minecraft:cardinal_direction", CardinalDirection.class, ImmutableSet.of(CardinalDirection.SOUTH, CardinalDirection.WEST, CardinalDirection.NORTH, CardinalDirection.EAST), CardinalDirection.SOUTH);
    public static final EnumBlockTrait<CardinalDirection> SIGN_DIRECTION = EnumBlockTrait.of("sign_direction", "ground_sign_direction", CardinalDirection.class);
    public static final EnumBlockTrait<Direction> DIRECTION = EnumBlockTrait.of("direction", Direction.class, Direction.EAST, Direction.SOUTH, Direction.WEST, Direction.NORTH);
    public static final EnumBlockTrait<Direction> FACING_DIRECTION = EnumBlockTrait.of("facing_direction", Direction.class, Direction.DOWN, Direction.EAST, Direction.WEST, Direction.SOUTH, Direction.NORTH, Direction.UP);
    public static final EnumBlockTrait<LeverDirection> LEVER_DIRECTION = EnumBlockTrait.of("lever_direction", LeverDirection.class);
    public static final EnumBlockTrait<Direction.Axis> PORTAL_AXIS = EnumBlockTrait.of("portal_axis", Direction.Axis.class,
            Direction.Axis.Y, // Palette value "unknown"
            Direction.Axis.X, Direction.Axis.Z);
    public static final EnumBlockTrait<RailDirection> RAIL_DIRECTION = EnumBlockTrait.of("rail_direction", RailDirection.class);
    public static final EnumBlockTrait<RailDirection> SIMPLE_RAIL_DIRECTION = EnumBlockTrait.of("simple_rail_direction", "rail_direction", RailDirection.class, ImmutableSet.copyOf(RailDirection.simpleValues()), RailDirection.NORTH_SOUTH);
    public static final EnumBlockTrait<Direction> TORCH_DIRECTION = EnumBlockTrait.of("torch_direction", "torch_facing_direction", Direction.class,
            Direction.DOWN, // Palette value "unknown"
            Direction.WEST, Direction.EAST, Direction.NORTH, Direction.SOUTH, Direction.UP);

    public static final EnumBlockTrait<AttachmentType> ATTACHMENT = EnumBlockTrait.of("attachment", AttachmentType.class);
    public static final EnumBlockTrait<BambooLeafSize> BAMBOO_LEAF_SIZE = EnumBlockTrait.of("bamboo_leaf_size", BambooLeafSize.class);
    public static final EnumBlockTrait<BambooStalkThickness> BAMBOO_STALK_THICKNESS = EnumBlockTrait.of("bamboo_stalk_thickness", BambooStalkThickness.class);
    public static final EnumBlockTrait<Bucket> CAULDRON_TYPE = EnumBlockTrait.of("cauldron_type", "cauldron_liquid", Bucket.class, Bucket.WATER, Bucket.LAVA, Bucket.POWDER_SNOW);
    public static final EnumBlockTrait<CrackedState> CRACKED_STATE = EnumBlockTrait.of("cracked_state", CrackedState.class);
    public static final EnumBlockTrait<CrafterOrientation> CRAFTER_ORIENTATION = EnumBlockTrait.of("orientation", CrafterOrientation.class);
    public static final EnumBlockTrait<CreakingHeartState> CREAKING_HEART_STATE = EnumBlockTrait.of("creaking_heart_state", CreakingHeartState.class);
    public static final EnumBlockTrait<DripleafTilt> DRIPLEAF_TILT = EnumBlockTrait.of("dripleaf_tilt", "big_dripleaf_tilt", DripleafTilt.class);
    public static final EnumBlockTrait<DripstoneThickness> DRIPSTONE_THICKNESS = EnumBlockTrait.of("dripstone_thickness", DripstoneThickness.class);
    public static final EnumBlockTrait<PotentSulfurState> POTENT_SULFUR_STATE = EnumBlockTrait.of("potent_sulfur_state", PotentSulfurState.class);
    public static final EnumBlockTrait<WallConnectionType> PALE_MOSS_CARPET_SIDE_EAST = EnumBlockTrait.of("pale_moss_carpet_side_east", WallConnectionType.class);
    public static final EnumBlockTrait<WallConnectionType> PALE_MOSS_CARPET_SIDE_NORTH = EnumBlockTrait.of("pale_moss_carpet_side_north", WallConnectionType.class);
    public static final EnumBlockTrait<WallConnectionType> PALE_MOSS_CARPET_SIDE_SOUTH = EnumBlockTrait.of("pale_moss_carpet_side_south", WallConnectionType.class);
    public static final EnumBlockTrait<WallConnectionType> PALE_MOSS_CARPET_SIDE_WEST = EnumBlockTrait.of("pale_moss_carpet_side_west", WallConnectionType.class);
    public static final EnumBlockTrait<SeaGrassType> SEA_GRASS_TYPE = EnumBlockTrait.of("sea_grass_type", SeaGrassType.class);
    public static final EnumBlockTrait<SlabSlot> SLAB_SLOT = EnumBlockTrait.of("slab_slot", SlabSlot.class);
    public static final EnumBlockTrait<StructureBlockType> STRUCTURE_BLOCK_TYPE = EnumBlockTrait.of("structure_block_type", StructureBlockType.class);
    public static final EnumBlockTrait<TurtleEggCount> TURTLE_EGG_COUNT = EnumBlockTrait.of("turtle_egg_count", TurtleEggCount.class);
    public static final EnumBlockTrait<VaultState> VAULT_STATE = EnumBlockTrait.of("vault_state", VaultState.class);
    public static final EnumBlockTrait<WallConnectionType> WALL_CONNECTION_EAST = EnumBlockTrait.of("wall_connection_east", "wall_connection_type_east", WallConnectionType.class);
    public static final EnumBlockTrait<WallConnectionType> WALL_CONNECTION_NORTH = EnumBlockTrait.of("wall_connection_north", "wall_connection_type_north", WallConnectionType.class);
    public static final EnumBlockTrait<WallConnectionType> WALL_CONNECTION_SOUTH = EnumBlockTrait.of("wall_connection_south", "wall_connection_type_south", WallConnectionType.class);
    public static final EnumBlockTrait<WallConnectionType> WALL_CONNECTION_WEST = EnumBlockTrait.of("wall_connection_west", "wall_connection_type_west", WallConnectionType.class);

    public static final IntegerBlockTrait AGE = IntegerBlockTrait.from("age", 16);
    public static final IntegerBlockTrait BITE_COUNTER = IntegerBlockTrait.from("bite_counter", 7);
    public static final IntegerBlockTrait BOOKS_STORED = IntegerBlockTrait.from("books_stored", 64);
    public static final IntegerBlockTrait BRUSHED_PROGRESS = IntegerBlockTrait.from("brushed_progress", 4);
    public static final IntegerBlockTrait CANDLES = IntegerBlockTrait.from("candles", 0, 3);
    public static final IntegerBlockTrait CAVE_VINE_AGE = IntegerBlockTrait.from("growing_plant_age", 26);
    public static final IntegerBlockTrait CHORUS_AGE = IntegerBlockTrait.from("chorus_age", "age", 0, 5, 0);
    public static final IntegerBlockTrait CLUSTER_COUNT = IntegerBlockTrait.from("cluster_count", 4);
    public static final IntegerBlockTrait COMPOSTER_FILL_LEVEL = IntegerBlockTrait.from("composter_fill_level", 9);
    public static final IntegerBlockTrait CORAL_DIRECTION = IntegerBlockTrait.from("coral_direction", 4);
    public static final IntegerBlockTrait CORAL_FAN_DIRECTION = IntegerBlockTrait.from("coral_fan_direction", 2);
    public static final IntegerBlockTrait FILL_LEVEL = IntegerBlockTrait.from("fill_level", 7);
    public static final IntegerBlockTrait FLUID_LEVEL = IntegerBlockTrait.from("fluid_level", 8);
    public static final IntegerBlockTrait GROWTH = IntegerBlockTrait.from("growth", 8);
    public static final IntegerBlockTrait HEIGHT = IntegerBlockTrait.from("height", 8);
    public static final IntegerBlockTrait HONEY_LEVEL = IntegerBlockTrait.from("honey_level", 6);
    public static final IntegerBlockTrait HUGE_MUSHROOM_BITS = IntegerBlockTrait.from("huge_mushroom_bits", 16);
    public static final IntegerBlockTrait JIGSAW_ROTATION = IntegerBlockTrait.from("rotation", 4);
    public static final IntegerBlockTrait KELP_AGE = IntegerBlockTrait.from("kelp_age", 26);
    public static final IntegerBlockTrait LIQUID_DEPTH = IntegerBlockTrait.from("liquid_depth", 16);
    public static final IntegerBlockTrait MOISTURIZED_AMOUNT = IntegerBlockTrait.from("moisturized_amount", 8);
    public static final IntegerBlockTrait MULTI_FACE_DIRECTION = IntegerBlockTrait.from("multi_face_direction", "multi_face_direction_bits", 0, 63, 0);
    public static final IntegerBlockTrait POWERED_SHELF_TYPE = IntegerBlockTrait.from("powered_shelf_type", 4);
    public static final IntegerBlockTrait PROPAGULE_STAGE = IntegerBlockTrait.from("propagule_stage", 5);
    public static final IntegerBlockTrait REDSTONE_SIGNAL = IntegerBlockTrait.from("redstone_signal", 16);
    public static final IntegerBlockTrait REHYDRATION_LEVEL = IntegerBlockTrait.from("rehydration_level", 4);
    public static final IntegerBlockTrait REPEATER_DELAY = IntegerBlockTrait.from("repeater_delay", 4);
    public static final IntegerBlockTrait RESPAWN_ANCHOR_CHARGE = IntegerBlockTrait.from("respawn_anchor_charge", 5);
    public static final IntegerBlockTrait SCULK_SENSOR_PHASE = IntegerBlockTrait.from("sculk_sensor_phase", 3);
    public static final IntegerBlockTrait STABILITY = IntegerBlockTrait.from("stability", 8);
    public static final IntegerBlockTrait TRIAL_SPAWNER_STATE = IntegerBlockTrait.from("trial_spawner_state", 6);
    public static final IntegerBlockTrait TWISTING_VINES_AGE = IntegerBlockTrait.from("twisting_vines_age", 26);
    public static final IntegerBlockTrait VINE_DIRECTION_BITS = IntegerBlockTrait.from("vine_direction_bits", 16);
    public static final IntegerBlockTrait WEEPING_VINES_AGE = IntegerBlockTrait.from("weeping_vines_age", 26);

    public static final BooleanBlockTrait CAN_SUMMON = BooleanBlockTrait.of("can_summon");
    public static final BooleanBlockTrait CRAFTER_CRAFTING = BooleanBlockTrait.of("crafter_crafting", "crafting");
    public static final BooleanBlockTrait CRAFTER_TRIGGERED = BooleanBlockTrait.of("crafter_triggered", "triggered_bit");
    public static final BooleanBlockTrait DRIPLEAF_HEAD = BooleanBlockTrait.of("dripleaf_head", "big_dripleaf_head");
    public static final BooleanBlockTrait EXPLODE = BooleanBlockTrait.of("explode", "explode_bit");
    public static final BooleanBlockTrait HAS_AGE = BooleanBlockTrait.of("has_age", "age_bit");
    public static final BooleanBlockTrait HAS_DRAG_DOWN = BooleanBlockTrait.of("drag_down");
    public static final BooleanBlockTrait HAS_END_PORTAL_EYE = BooleanBlockTrait.of("has_end_portal_eye", "end_portal_eye_bit");
    public static final BooleanBlockTrait HAS_INFINIBURN = BooleanBlockTrait.of("has_infiniburn", "infiniburn_bit");
    public static final BooleanBlockTrait HAS_MAP = BooleanBlockTrait.of("has_map", "item_frame_map_bit");
    public static final BooleanBlockTrait HAS_PHOTO = BooleanBlockTrait.of("has_photo", "item_frame_photo_bit");
    public static final BooleanBlockTrait HAS_POST = BooleanBlockTrait.of("has_post", "wall_post_bit");
    public static final BooleanBlockTrait HAS_STABILITY_CHECK = BooleanBlockTrait.of("has_stability_check", "stability_check");
    public static final BooleanBlockTrait HAS_UPDATE = BooleanBlockTrait.of("has_update", "update_bit");
    public static final BooleanBlockTrait IS_ACTIVE = BooleanBlockTrait.of("is_active", "active");
    public static final BooleanBlockTrait IS_ATTACHED = BooleanBlockTrait.of("is_attached", "attached_bit");
    public static final BooleanBlockTrait IS_BLOOMING = BooleanBlockTrait.of("is_blooming", "bloom");
    public static final BooleanBlockTrait IS_BREWING_A = BooleanBlockTrait.of("is_brewing_a", "brewing_stand_slot_a_bit");
    public static final BooleanBlockTrait IS_BREWING_B = BooleanBlockTrait.of("is_brewing_b", "brewing_stand_slot_b_bit");
    public static final BooleanBlockTrait IS_BREWING_C = BooleanBlockTrait.of("is_brewing_c", "brewing_stand_slot_c_bit");
    public static final BooleanBlockTrait IS_BUTTON_PRESSED = BooleanBlockTrait.of("is_button_pressed", "button_pressed_bit");
    public static final BooleanBlockTrait IS_CONDITIONAL = BooleanBlockTrait.of("is_conditional", "conditional_bit");
    public static final BooleanBlockTrait IS_COVERED = BooleanBlockTrait.of("is_covered", "covered_bit");
    public static final BooleanBlockTrait IS_DEAD = BooleanBlockTrait.of("is_dead", "dead_bit");
    public static final BooleanBlockTrait IS_DISARMED = BooleanBlockTrait.of("is_disarmed", "disarmed_bit");
    public static final BooleanBlockTrait IS_DOOR_HINGE = BooleanBlockTrait.of("is_door_hinge", "door_hinge_bit", true);
    public static final BooleanBlockTrait IS_EXTINGUISHED = BooleanBlockTrait.of("is_extinguished", "extinguished");
    public static final BooleanBlockTrait IS_HANGING = BooleanBlockTrait.of("is_hanging", "hanging");
    public static final BooleanBlockTrait IS_HEAD_PIECE = BooleanBlockTrait.of("is_head_piece", "head_piece_bit");
    public static final BooleanBlockTrait IS_IN_WALL = BooleanBlockTrait.of("is_in_wall", "in_wall_bit");
    public static final BooleanBlockTrait IS_LIT = BooleanBlockTrait.of("is_lit", "lit");
    public static final BooleanBlockTrait IS_OCCUPIED = BooleanBlockTrait.of("is_occupied", "occupied_bit");
    public static final BooleanBlockTrait IS_OMINOUS = BooleanBlockTrait.of("is_ominous", "ominous");
    public static final BooleanBlockTrait IS_OPEN = BooleanBlockTrait.of("is_open", "open_bit");
    public static final BooleanBlockTrait IS_OUTPUT_LIT = BooleanBlockTrait.of("is_output_lit", "output_lit_bit");
    public static final BooleanBlockTrait IS_OUTPUT_SUBTRACT = BooleanBlockTrait.of("is_output_subtract", "output_subtract_bit");
    public static final BooleanBlockTrait IS_PERSISTENT = BooleanBlockTrait.of("is_persistent", "persistent_bit");
    public static final BooleanBlockTrait IS_POWERED = BooleanBlockTrait.of("is_powered", "powered_bit");
    public static final BooleanBlockTrait IS_SUSPENDED = BooleanBlockTrait.of("is_suspended", "suspended_bit");
    public static final BooleanBlockTrait IS_TOGGLED = BooleanBlockTrait.of("is_toggled", "toggle_bit");
    public static final BooleanBlockTrait IS_TRIGGERED = BooleanBlockTrait.of("is_triggered", "triggered_bit");
    public static final BooleanBlockTrait IS_UPPER_BLOCK = BooleanBlockTrait.of("is_upper_block", "upper_block_bit");
    public static final BooleanBlockTrait IS_UPSIDE_DOWN = BooleanBlockTrait.of("is_upside_down", "upside_down_bit");
    public static final BooleanBlockTrait NATURAL = BooleanBlockTrait.of("natural");
    public static final BooleanBlockTrait TIP = BooleanBlockTrait.of("tip");

    public static final IntegerBlockTrait CHALKBOARD_DIRECTION = IntegerBlockTrait.from("chalkboard_direction", "direction", 0, 15, 0);
    public static final IntegerBlockTrait CHEMISTRY_TABLE_DIRECTION = IntegerBlockTrait.from("chemistry_table_direction", "direction", 0, 3, 0);

    public static final IntegerBlockTrait DEPRECATED = IntegerBlockTrait.from("deprecated", 4);

    @SuppressWarnings({"unchecked", "ConstantConditions"})
    private static <T extends Enum<T>> T[] getEnumValues(Class<T> value, T... except) {
        var set = Sets.newHashSet(except);
        var values = value.getEnumConstants();
        var stream = Arrays.stream(values).filter(v -> !set.contains(v));

        return stream.toArray((s) -> (T[]) Array.newInstance(value, s));
    }
}
