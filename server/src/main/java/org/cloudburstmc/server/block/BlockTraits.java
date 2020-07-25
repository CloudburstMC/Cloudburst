package org.cloudburstmc.server.block;

import it.unimi.dsi.fastutil.objects.Object2ReferenceMap;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import lombok.experimental.UtilityClass;
import org.cloudburstmc.server.block.trait.BlockTrait;
import org.cloudburstmc.server.block.trait.BooleanBlockTrait;
import org.cloudburstmc.server.block.trait.EnumBlockTrait;
import org.cloudburstmc.server.block.trait.IntegerBlockTrait;
import org.cloudburstmc.server.math.Direction;
import org.cloudburstmc.server.utils.data.*;

import javax.annotation.Nonnull;

@UtilityClass
public class BlockTraits {

    private final Object2ReferenceMap<String, BlockTrait<?>> vanillaMapping = new Object2ReferenceOpenHashMap<>();
    private final Object2ReferenceMap<String, BlockTrait<?>> internalMapping = new Object2ReferenceOpenHashMap<>();

    public final IntegerBlockTrait AGE = IntegerBlockTrait.from("age", 16);
    public final EnumBlockTrait<AttachmentType> ATTACHMENT = EnumBlockTrait.of("attachment", AttachmentType.class);
    public final EnumBlockTrait<BambooLeafSize> BAMBOO_LEAF_SIZE = EnumBlockTrait.of("bamboo_leaf_size", BambooLeafSize.class);
    public final EnumBlockTrait<BambooStalkThickness> BAMBOO_STALK_THICKNESS = EnumBlockTrait.of("bamboo_stalk_thickness", BambooStalkThickness.class);
    public final IntegerBlockTrait BITE_COUNTER = IntegerBlockTrait.from("bite_counter", 7);
    public final IntegerBlockTrait LIGHT_LEVEL = IntegerBlockTrait.from("light_level", 16);
    public final EnumBlockTrait<FluidType> FLUID_TYPE = EnumBlockTrait.of("fluid_type", FluidType.class);
    public final EnumBlockTrait<ChemistryTableType> CHEMISTRY_TABLE_TYPE = EnumBlockTrait.of("chemistry_table_type", ChemistryTableType.class);
    public final EnumBlockTrait<ChiselType> CHISEL_TYPE = EnumBlockTrait.of("chisel_type", ChiselType.class);
    public final IntegerBlockTrait CLUSTER_COUNT = IntegerBlockTrait.from("cluster_count", 4);
    public final EnumBlockTrait<DyeColor> COLOR = EnumBlockTrait.of("color", DyeColor.class);
    public final IntegerBlockTrait COMPOSTER_FILL_LEVEL = IntegerBlockTrait.from("composter_fill_level", 9);
    public final EnumBlockTrait<DyeColor> CORAL_COLOR = EnumBlockTrait.of("coral_color", DyeColor.class,
            DyeColor.BLUE, DyeColor.PINK, DyeColor.PURPLE, DyeColor.RED, DyeColor.YELLOW);
    public final EnumBlockTrait<CrackedState> CRACKED_STATE = EnumBlockTrait.of("cracked_state", CrackedState.class);
    public final EnumBlockTrait<AnvilDamage> DAMAGE = EnumBlockTrait.of("damage", AnvilDamage.class);
    public final EnumBlockTrait<DirtType> DIRT_TYPE = EnumBlockTrait.of("dirt_type", DirtType.class);
    public final EnumBlockTrait<DoublePlantType> DOUBLE_PLANT_TYPE = EnumBlockTrait.of("double_plant_type", DoublePlantType.class);
    public final BooleanBlockTrait DRAG_DOWN = BooleanBlockTrait.of("drag_down");
    public final BooleanBlockTrait EXPLODE = BooleanBlockTrait.of("explode", "explode_bit");
    public final IntegerBlockTrait FILL_LEVEL = IntegerBlockTrait.from("fill_level", 7);
    public final EnumBlockTrait<FlowerType> FLOWER_TYPE = EnumBlockTrait.of("flower_type", FlowerType.class);
    public final IntegerBlockTrait FLUID_LEVEL = IntegerBlockTrait.from("fluid_level", 8);
    public final IntegerBlockTrait GROWTH = IntegerBlockTrait.from("growth", 8);
    public final IntegerBlockTrait HEIGHT = IntegerBlockTrait.from("height", 8);
    public final IntegerBlockTrait HONEY_LEVEL = IntegerBlockTrait.from("honey_level", 8);
    public final IntegerBlockTrait KELP_AGE = IntegerBlockTrait.from("kelp_age", 26);
    public final IntegerBlockTrait MOISTURIZED_AMOUNT = IntegerBlockTrait.from("moisturized_amount", 8);
    public final EnumBlockTrait<PrismarineBlockType> PRISMARINE_BLOCK_TYPE = EnumBlockTrait.of("prismarine_block_type", PrismarineBlockType.class);
    public final IntegerBlockTrait REDSTONE_SIGNAL = IntegerBlockTrait.from("redstone_signal", 16);
    public final IntegerBlockTrait REPEATER_DELAY = IntegerBlockTrait.from("repeater_delay", 4);
    public final IntegerBlockTrait RESPAWN_ANCHOR_CHARGE = IntegerBlockTrait.from("respawn_anchor_charge", 5);
    public final EnumBlockTrait<SandStoneType> SAND_STONE_TYPE = EnumBlockTrait.of("sand_stone_type", SandStoneType.class);
    public final EnumBlockTrait<SandType> SAND_TYPE = EnumBlockTrait.of("sand_type", SandType.class);
    public final EnumBlockTrait<SeaGrassType> SEA_GRASS_TYPE = EnumBlockTrait.of("sea_gras_type", SeaGrassType.class);
    public final EnumBlockTrait<SpongeType> SPONGE_TYPE = EnumBlockTrait.of("sponge_type", SpongeType.class);
    public final IntegerBlockTrait STABILITY = IntegerBlockTrait.from("stability", 8);
    public final EnumBlockTrait<StoneType> STONE_TYPE = EnumBlockTrait.of("stone_type", StoneType.class);
    public final EnumBlockTrait<StoneBrickType> STONE_BRICK_TYPE = EnumBlockTrait.of("stone_brick_type", StoneBrickType.class);
    public final EnumBlockTrait<StoneSlabType> STONE_SLAB_TYPE = EnumBlockTrait.of("stone_slab_type", StoneSlabType.class);
    public final EnumBlockTrait<StructureBlockType> STRUCTURE_BLOCK_TYPE = EnumBlockTrait.of("structure_block_type", StructureBlockType.class);
    public final EnumBlockTrait<StructureVoidType> STRUCTURE_VOID_TYPE = EnumBlockTrait.of("structure_void_type", StructureVoidType.class);
    public final EnumBlockTrait<TallGrassType> TALL_GRASS_TYPE = EnumBlockTrait.of("tall_grass_type", TallGrassType.class);
    public final EnumBlockTrait<TreeSpecies> TREE_SPECIES = EnumBlockTrait.of("tree_species", TreeSpecies.class);
    public final IntegerBlockTrait TURTLE_EGG_COUNT = IntegerBlockTrait.from("turtle_egg_count", 1, 4);
    public final IntegerBlockTrait TWISTING_VINES_AGE = IntegerBlockTrait.from("twisting_vines_age", 26);
    public final EnumBlockTrait<WallConnectionType> WALL_CONNECTION_NORTH = EnumBlockTrait.of("wall_connection_north", WallConnectionType.class);
    public final EnumBlockTrait<WallConnectionType> WALL_CONNECTION_EAST = EnumBlockTrait.of("wall_connection_east", WallConnectionType.class);
    public final EnumBlockTrait<WallConnectionType> WALL_CONNECTION_SOUTH = EnumBlockTrait.of("wall_connection_south", WallConnectionType.class);
    public final EnumBlockTrait<WallConnectionType> WALL_CONNECTION_WEST = EnumBlockTrait.of("wall_connection_west", WallConnectionType.class);
    public final IntegerBlockTrait WEEPING_VINES_AGE = IntegerBlockTrait.from("weeping_vines_age", 26);

    public final EnumBlockTrait<Direction> FACING_DIRECTION = EnumBlockTrait.of("facing_direction", Direction.class, Direction.UP, Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST);
    public final EnumBlockTrait<Direction> DIRECTION = EnumBlockTrait.of("direction", Direction.class, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST);

    public final BooleanBlockTrait IS_ALLOWED_UNDERWATER = BooleanBlockTrait.of("is_allowed_underwater", "allow_underwater_bit");
    public final BooleanBlockTrait IS_ATTACHED = BooleanBlockTrait.of("is_attached", "attached_bit");
    public final BooleanBlockTrait IS_BUTTON_PRESSED = BooleanBlockTrait.of("is_button_pressed", "button_pressed_bit");
    public final BooleanBlockTrait IS_CONDITIONAL = BooleanBlockTrait.of("is_conditional", "conditional_bit");
    public final BooleanBlockTrait IS_COVERED = BooleanBlockTrait.of("is_covered", "covered_bit");
    public final BooleanBlockTrait IS_DEAD = BooleanBlockTrait.of("is_dead", "dead_bit");
    public final BooleanBlockTrait IS_DISARMED = BooleanBlockTrait.of("is_disarmed", "disarmed_bit");
    public final BooleanBlockTrait IS_DOOR_HINGE = BooleanBlockTrait.of("is_door_hinge", "door_hinge_bit");
    public final BooleanBlockTrait IS_EXTINGUISHED = BooleanBlockTrait.of("is_extinguished", "extinguished");
    public final BooleanBlockTrait IS_FLOWING = BooleanBlockTrait.of("is_flowing");
    public final BooleanBlockTrait IS_HANGING = BooleanBlockTrait.of("is_hanging", "hanging");
    public final BooleanBlockTrait IS_HEAD_PIECE = BooleanBlockTrait.of("is_head_piece", "head_piece_bit");
    public final BooleanBlockTrait IS_IN_WALL = BooleanBlockTrait.of("is_in_wall", "in_wall_bit");
    public final BooleanBlockTrait IS_OCCUPIED = BooleanBlockTrait.of("is_occupied", "occupied_bit");
    public final BooleanBlockTrait IS_OPEN = BooleanBlockTrait.of("is_open", "open_bit");
    public final BooleanBlockTrait IS_OUTPUT_LIT = BooleanBlockTrait.of("is_output_lit", "output_lit_bit");
    public final BooleanBlockTrait IS_OUTPUT_SUBTRACT = BooleanBlockTrait.of("is_output_subtract", "output_subtract_bit");
    public final BooleanBlockTrait IS_PERSISTENT = BooleanBlockTrait.of("is_persistent", "persistent_bit");
    public final BooleanBlockTrait IS_POWERED = BooleanBlockTrait.of("is_powered", "powered_bit");
    public final BooleanBlockTrait IS_STRIPPED = BooleanBlockTrait.of("is_stripped", "stripped_bit");
    public final BooleanBlockTrait IS_SUSPENDED = BooleanBlockTrait.of("is_suspended", "suspended_bit");
    public final BooleanBlockTrait IS_TOGGLED = BooleanBlockTrait.of("is_toggled", "toggle_bit");
    public final BooleanBlockTrait IS_TOP_SLOT = BooleanBlockTrait.of("is_top_slot", "top_slot_bit");
    public final BooleanBlockTrait IS_TRIGGERED = BooleanBlockTrait.of("is_triggered", "triggered_bit");
    public final BooleanBlockTrait IS_UPPER_BLOCK = BooleanBlockTrait.of("is_upper_block", "upper_block_bit");
    public final BooleanBlockTrait IS_UPSIDE_DOWN = BooleanBlockTrait.of("is_upside_down", "upside_down_bit");
    public final BooleanBlockTrait IS_WALL_POST = BooleanBlockTrait.of("is_wall_post", "wall_post_bit");
    public final BooleanBlockTrait HAS_AGE = BooleanBlockTrait.of("has_age", "age");
    public final BooleanBlockTrait HAS_BREWING_STAND_SLOT_A = BooleanBlockTrait.of("has_brewing_stand_slot_a", "has_brewing_stand_slot_a_bit");
    public final BooleanBlockTrait HAS_BREWING_STAND_SLOT_B = BooleanBlockTrait.of("has_brewing_stand_slot_b", "has_brewing_stand_slot_b_bit");
    public final BooleanBlockTrait HAS_BREWING_STAND_SLOT_C = BooleanBlockTrait.of("has_brewing_stand_slot_c", "has_brewing_stand_slot_c_bit");
    public final BooleanBlockTrait HAS_COLOR = BooleanBlockTrait.of("has_color", "color");
    public final BooleanBlockTrait HAS_END_PORTAL_EYE = BooleanBlockTrait.of("has_end_portal_eye", "end_portal_eye_bit");
    public final BooleanBlockTrait HAS_INFINIBURN = BooleanBlockTrait.of("has_infiniburn", "infiniburn_bit");
    public final BooleanBlockTrait HAS_NO_DROP = BooleanBlockTrait.of("has_no_drop", "no_drop_bit");
    public final BooleanBlockTrait HAS_UPDATE = BooleanBlockTrait.of("has_update", "update_bit");

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
