package org.cloudburstmc.server.block;

import com.google.common.collect.ImmutableSet;
import it.unimi.dsi.fastutil.objects.Object2ReferenceMap;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import lombok.experimental.UtilityClass;
import org.cloudburstmc.server.block.trait.BlockTrait;
import org.cloudburstmc.server.block.trait.BooleanBlockTrait;
import org.cloudburstmc.server.block.trait.EnumBlockTrait;
import org.cloudburstmc.server.block.trait.IntegerBlockTrait;
import org.cloudburstmc.server.item.data.Bucket;
import org.cloudburstmc.server.math.Direction;
import org.cloudburstmc.server.math.Direction.Axis;
import org.cloudburstmc.server.math.LeverDirection;
import org.cloudburstmc.server.utils.data.*;

import javax.annotation.Nonnull;

import static org.cloudburstmc.server.block.serializer.util.BedrockStateTags.TAG_CORAL_HANG_TYPE_BIT;

@SuppressWarnings("RedundantModifiersUtilityClassLombok")
@UtilityClass
public class BlockTraits {

    private static final Object2ReferenceMap<String, BlockTrait<?>> vanillaMapping = new Object2ReferenceOpenHashMap<>();
    private static final Object2ReferenceMap<String, BlockTrait<?>> internalMapping = new Object2ReferenceOpenHashMap<>();

    public static final IntegerBlockTrait AGE = IntegerBlockTrait.from("age", 16);
    public static final EnumBlockTrait<AttachmentType> ATTACHMENT = EnumBlockTrait.of("attachment", AttachmentType.class);
    public static final EnumBlockTrait<BambooLeafSize> BAMBOO_LEAF_SIZE = EnumBlockTrait.of("bamboo_leaf_size", BambooLeafSize.class);
    public static final EnumBlockTrait<BambooStalkThickness> BAMBOO_STALK_THICKNESS = EnumBlockTrait.of("bamboo_stalk_thickness", BambooStalkThickness.class);
    public static final IntegerBlockTrait BITE_COUNTER = IntegerBlockTrait.from("bite_counter", 7);
    public static final EnumBlockTrait<Bucket> CAULDRON_TYPE = EnumBlockTrait.of("cauldron_type", Bucket.class, Bucket.WATER, Bucket.LAVA);
    public static final IntegerBlockTrait CHORUS_AGE = IntegerBlockTrait.from("chorus_age", "age", 0, 5, 0);
    public static final IntegerBlockTrait COCOA_AGE = IntegerBlockTrait.from("cocoa_age", "age", 0, 2, 0);
    public static final IntegerBlockTrait CORAL_FAN_DIRECTION = IntegerBlockTrait.from("coral_fan_direction", 2);
    public static final EnumBlockTrait<ChemistryTableType> CHEMISTRY_TABLE_TYPE = EnumBlockTrait.of("chemistry_table_type", ChemistryTableType.class);
    public static final EnumBlockTrait<ChiselType> CHISEL_TYPE = EnumBlockTrait.of("chisel_type", ChiselType.class);
    public static final IntegerBlockTrait CLUSTER_COUNT = IntegerBlockTrait.from("cluster_count", 4);
    public static final EnumBlockTrait<DyeColor> COLOR = EnumBlockTrait.of("color", DyeColor.class);
    public static final IntegerBlockTrait COMPOSTER_FILL_LEVEL = IntegerBlockTrait.from("composter_fill_level", 9);
    public static final EnumBlockTrait<DyeColor> CORAL_COLOR = EnumBlockTrait.of("coral_color", DyeColor.class,
            DyeColor.BLUE, DyeColor.PINK, DyeColor.PURPLE, DyeColor.RED, DyeColor.YELLOW);
    public static final EnumBlockTrait<DyeColor> CORAL_HANG_COLOR = EnumBlockTrait.of("coral_color", TAG_CORAL_HANG_TYPE_BIT, DyeColor.class,
            DyeColor.BLUE, DyeColor.PINK, DyeColor.PURPLE, DyeColor.RED, DyeColor.YELLOW, DyeColor.ORANGE); // Last value is fake.
    public static final BooleanBlockTrait CORAL_HANG_TYPE = BooleanBlockTrait.of("coral_hang_type", "coral_hang_type_bit");
    public static final EnumBlockTrait<CrackedState> CRACKED_STATE = EnumBlockTrait.of("cracked_state", CrackedState.class);
    public static final EnumBlockTrait<AnvilDamage> DAMAGE = EnumBlockTrait.of("damage", AnvilDamage.class, AnvilDamage.UNDAMAGED);
    public static final EnumBlockTrait<DirtType> DIRT_TYPE = EnumBlockTrait.of("dirt_type", DirtType.class);
    public static final EnumBlockTrait<DoublePlantType> DOUBLE_PLANT_TYPE = EnumBlockTrait.of("double_plant_type", DoublePlantType.class);
    public static final BooleanBlockTrait HAS_DRAG_DOWN = BooleanBlockTrait.of("drag_down");
    public static final BooleanBlockTrait EXPLODE = BooleanBlockTrait.of("explode", "explode_bit");
    public static final EnumBlockTrait<FlowerType> FLOWER_TYPE = EnumBlockTrait.of("flower_type", FlowerType.class);
    public static final IntegerBlockTrait FLUID_LEVEL = IntegerBlockTrait.from("fluid_level", 8);
    public static final EnumBlockTrait<FluidType> FLUID_TYPE = EnumBlockTrait.of("fluid_type", FluidType.class);
    public static final IntegerBlockTrait FILL_LEVEL = IntegerBlockTrait.from("fill_level", 7);
    public static final IntegerBlockTrait GROWTH = IntegerBlockTrait.from("growth", 8);
    public static final IntegerBlockTrait HEIGHT = IntegerBlockTrait.from("height", 8);
    public static final IntegerBlockTrait HONEY_LEVEL = IntegerBlockTrait.from("honey_level", 6);
    public static final IntegerBlockTrait HUGE_MUSHROOM_BITS = IntegerBlockTrait.from("huge_mushroom_bits", 16);
    public static final IntegerBlockTrait ICE_AGE = IntegerBlockTrait.from("ice_age", "age", 0, 3, 0);
    public static final IntegerBlockTrait KELP_AGE = IntegerBlockTrait.from("kelp_age", 26);
    public static final IntegerBlockTrait LIGHT_LEVEL = IntegerBlockTrait.from("light_level", "block_light_level", 0, 15, 0);
    public static final IntegerBlockTrait MOISTURIZED_AMOUNT = IntegerBlockTrait.from("moisturized_amount", 8);
    public static final EnumBlockTrait<MonsterEggStoneType> MONSTER_EGG_STONE_TYPE = EnumBlockTrait.of("monster_egg_stone_type", MonsterEggStoneType.class);
    public static final IntegerBlockTrait WART_GROWTH = IntegerBlockTrait.from("wart_growth", "age", 0, 3, 0);
    public static final EnumBlockTrait<PrismarineBlockType> PRISMARINE_BLOCK_TYPE = EnumBlockTrait.of("prismarine_block_type", PrismarineBlockType.class);
    public static final IntegerBlockTrait REDSTONE_SIGNAL = IntegerBlockTrait.from("redstone_signal", 16);
    public static final IntegerBlockTrait REPEATER_DELAY = IntegerBlockTrait.from("repeater_delay", 4);
    public static final IntegerBlockTrait RESPAWN_ANCHOR_CHARGE = IntegerBlockTrait.from("respawn_anchor_charge", 5);
    public static final EnumBlockTrait<SandStoneType> SAND_STONE_TYPE = EnumBlockTrait.of("sand_stone_type", SandStoneType.class);
    public static final EnumBlockTrait<SandType> SAND_TYPE = EnumBlockTrait.of("sand_type", SandType.class);
    public static final EnumBlockTrait<SeaGrassType> SEA_GRASS_TYPE = EnumBlockTrait.of("sea_grass_type", SeaGrassType.class);
    public static final EnumBlockTrait<SpongeType> SPONGE_TYPE = EnumBlockTrait.of("sponge_type", SpongeType.class);
    public static final IntegerBlockTrait STABILITY = IntegerBlockTrait.from("stability", 8);
    public static final EnumBlockTrait<StoneType> STONE_TYPE = EnumBlockTrait.of("stone_type", StoneType.class);
    public static final EnumBlockTrait<StoneBrickType> STONE_BRICK_TYPE = EnumBlockTrait.of("stone_brick_type", StoneBrickType.class);
    public static final EnumBlockTrait<StoneSlabType> STONE_SLAB_TYPE = EnumBlockTrait.of("stone_slab_type", StoneSlabType.class);
    public static final EnumBlockTrait<StructureBlockType> STRUCTURE_BLOCK_TYPE = EnumBlockTrait.of("structure_block_type", StructureBlockType.class);
    public static final EnumBlockTrait<StructureVoidType> STRUCTURE_VOID_TYPE = EnumBlockTrait.of("structure_void_type", StructureVoidType.class);
    public static final EnumBlockTrait<TallGrassType> TALL_GRASS_TYPE = EnumBlockTrait.of("tall_grass_type", TallGrassType.class);
    public static final EnumBlockTrait<TreeSpecies> TREE_SPECIES = EnumBlockTrait.of("tree_species", TreeSpecies.class);
    public static final EnumBlockTrait<TurtleEggCount> TURTLE_EGG_COUNT = EnumBlockTrait.of("turtle_egg_count", TurtleEggCount.class);
    public static final IntegerBlockTrait TWISTING_VINES_AGE = IntegerBlockTrait.from("twisting_vines_age", 26);
    public static final IntegerBlockTrait VINE_DIRECTION_BITS = IntegerBlockTrait.from("vine_direction_bits", 16);
    public static final EnumBlockTrait<WallBlockType> WALL_BLOCK_TYPE = EnumBlockTrait.of("wall_block_type", WallBlockType.class);
    public static final EnumBlockTrait<WallConnectionType> WALL_CONNECTION_NORTH = EnumBlockTrait.of("wall_connection_north", "wall_connection_type_north", WallConnectionType.class);
    public static final EnumBlockTrait<WallConnectionType> WALL_CONNECTION_EAST = EnumBlockTrait.of("wall_connection_east", "wall_connection_type_east", WallConnectionType.class);
    public static final EnumBlockTrait<WallConnectionType> WALL_CONNECTION_SOUTH = EnumBlockTrait.of("wall_connection_south", "wall_connection_type_south", WallConnectionType.class);
    public static final EnumBlockTrait<WallConnectionType> WALL_CONNECTION_WEST = EnumBlockTrait.of("wall_connection_west", "wall_connection_type_west", WallConnectionType.class);
    public static final IntegerBlockTrait WEEPING_VINES_AGE = IntegerBlockTrait.from("weeping_vines_age", 26);

    public static final EnumBlockTrait<Axis> AXIS = EnumBlockTrait.of("axis", "pillar_axis", Axis.class, ImmutableSet.copyOf(Axis.values()), Axis.Y);
    public static final EnumBlockTrait<Direction.Axis> PORTAL_AXIS = EnumBlockTrait.of("portal_axis", Direction.Axis.class,
            Direction.Axis.Y, //TODO: this was null
            Direction.Axis.X, Direction.Axis.Z);
    public static final EnumBlockTrait<Direction> FACING_DIRECTION = EnumBlockTrait.of("facing_direction", Direction.class, Direction.UP, Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST);
    public static final EnumBlockTrait<Direction> DIRECTION = EnumBlockTrait.of("direction", Direction.class, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST);
    public static final EnumBlockTrait<RailDirection> RAIL_DIRECTION = EnumBlockTrait.of("rail_direction", RailDirection.class);
    public static final EnumBlockTrait<RailDirection> SIMPLE_RAIL_DIRECTION = EnumBlockTrait.of("simple_rail_direction", "rail_direction", RailDirection.class, RailDirection.simpleValues());
    public static final EnumBlockTrait<Direction> TORCH_DIRECTION = EnumBlockTrait.of("torch_direction", "torch_facing_direction", Direction.class,
            Direction.DOWN, //TODO: this was null
            Direction.WEST, Direction.EAST, Direction.NORTH, Direction.SOUTH, Direction.UP);
    public static final EnumBlockTrait<CardinalDirection> CARDINAL_DIRECTION = EnumBlockTrait.of("cardinal_direction", "ground_sign_direction", CardinalDirection.class);
    public static final EnumBlockTrait<LeverDirection> LEVER_DIRECTION = EnumBlockTrait.of("lever_direction", LeverDirection.class);

    public static final BooleanBlockTrait IS_ALLOWED_UNDERWATER = BooleanBlockTrait.of("is_allowed_underwater", "allow_underwater_bit");
    public static final BooleanBlockTrait IS_ATTACHED = BooleanBlockTrait.of("is_attached", "attached_bit");
    public static final BooleanBlockTrait IS_BUTTON_PRESSED = BooleanBlockTrait.of("is_button_pressed", "button_pressed_bit");
    public static final BooleanBlockTrait IS_CONDITIONAL = BooleanBlockTrait.of("is_conditional", "conditional_bit");
    public static final BooleanBlockTrait IS_COVERED = BooleanBlockTrait.of("is_covered", "covered_bit");
    public static final BooleanBlockTrait IS_DEAD = BooleanBlockTrait.of("is_dead", "dead_bit");
    public static final BooleanBlockTrait IS_DISARMED = BooleanBlockTrait.of("is_disarmed", "disarmed_bit");
    public static final BooleanBlockTrait IS_DOOR_HINGE = BooleanBlockTrait.of("is_door_hinge", "door_hinge_bit", true);
    public static final BooleanBlockTrait IS_EXTINGUISHED = BooleanBlockTrait.of("is_extinguished", "extinguished");
    public static final BooleanBlockTrait IS_FLOWING = BooleanBlockTrait.of("is_flowing");
    public static final BooleanBlockTrait IS_HANGING = BooleanBlockTrait.of("is_hanging", "hanging");
    public static final BooleanBlockTrait IS_HEAD_PIECE = BooleanBlockTrait.of("is_head_piece", "head_piece_bit");
    public static final BooleanBlockTrait IS_IN_WALL = BooleanBlockTrait.of("is_in_wall", "in_wall_bit");
    public static final BooleanBlockTrait IS_OCCUPIED = BooleanBlockTrait.of("is_occupied", "occupied_bit");
    public static final BooleanBlockTrait IS_OPEN = BooleanBlockTrait.of("is_open", "open_bit");
    public static final BooleanBlockTrait IS_OUTPUT_LIT = BooleanBlockTrait.of("is_output_lit", "output_lit_bit");
    public static final BooleanBlockTrait IS_OUTPUT_SUBTRACT = BooleanBlockTrait.of("is_output_subtract", "output_subtract_bit");
    public static final BooleanBlockTrait IS_PERSISTENT = BooleanBlockTrait.of("is_persistent", "persistent_bit");
    public static final BooleanBlockTrait IS_POWERED = BooleanBlockTrait.of("is_powered", "powered_bit");
    public static final BooleanBlockTrait IS_SOUL = BooleanBlockTrait.of("is_soul");
    public static final BooleanBlockTrait IS_STRIPPED = BooleanBlockTrait.of("is_stripped", "stripped_bit");
    public static final BooleanBlockTrait IS_SUSPENDED = BooleanBlockTrait.of("is_suspended", "suspended_bit");
    public static final BooleanBlockTrait IS_TOGGLED = BooleanBlockTrait.of("is_toggled", "toggle_bit");
    public static final BooleanBlockTrait IS_TOP_SLOT = BooleanBlockTrait.of("is_top_slot", "top_slot_bit");
    public static final BooleanBlockTrait IS_TRIGGERED = BooleanBlockTrait.of("is_triggered", "triggered_bit");
    public static final BooleanBlockTrait IS_UPPER_BLOCK = BooleanBlockTrait.of("is_upper_block", "upper_block_bit");
    public static final BooleanBlockTrait IS_UPSIDE_DOWN = BooleanBlockTrait.of("is_upside_down", "upside_down_bit");
    public static final BooleanBlockTrait IS_BREWING_A = BooleanBlockTrait.of("is_brewing_a", "brewing_stand_slot_a_bit");
    public static final BooleanBlockTrait IS_BREWING_B = BooleanBlockTrait.of("is_brewing_b", "brewing_stand_slot_b_bit");
    public static final BooleanBlockTrait IS_BREWING_C = BooleanBlockTrait.of("is_brewing_c", "brewing_stand_slot_c_bit");
    public static final BooleanBlockTrait HAS_AGE = BooleanBlockTrait.of("has_age", "age_bit");
    public static final BooleanBlockTrait HAS_COLOR = BooleanBlockTrait.of("has_color", "color_bit");
    public static final BooleanBlockTrait HAS_END_PORTAL_EYE = BooleanBlockTrait.of("has_end_portal_eye", "end_portal_eye_bit");
    public static final BooleanBlockTrait HAS_INFINIBURN = BooleanBlockTrait.of("has_infiniburn", "infiniburn_bit");
    public static final BooleanBlockTrait HAS_NO_DROP = BooleanBlockTrait.of("has_no_drop", "no_drop_bit");
    public static final BooleanBlockTrait HAS_POST = BooleanBlockTrait.of("has_post", "wall_post_bit");
    public static final BooleanBlockTrait HAS_STABILITY_CHECK = BooleanBlockTrait.of("has_stability_check", "stability_check");
    public static final BooleanBlockTrait HAS_UPDATE = BooleanBlockTrait.of("has_update", "update_bit");
    public static final BooleanBlockTrait HAS_MAP = BooleanBlockTrait.of("has_map", "item_frame_map_bit");

    @Deprecated
    public final IntegerBlockTrait DEPRECATED = IntegerBlockTrait.from("deprecated", 4);

    public void register(@Nonnull BlockTrait<?> trait) {
        vanillaMapping.put(trait.getVanillaName(), trait);
        internalMapping.put(trait.getName(), trait);
    }

    public BlockTrait<?> fromVanilla(String name) {
        return vanillaMapping.get(name);
    }

    public BlockTrait<?> from(String name) {
        return internalMapping.get(name);
    }
}
