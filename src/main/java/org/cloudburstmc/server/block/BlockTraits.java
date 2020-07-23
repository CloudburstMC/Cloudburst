package org.cloudburstmc.server.block;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.server.block.trait.BooleanBlockTrait;
import org.cloudburstmc.server.block.trait.EnumBlockTrait;
import org.cloudburstmc.server.block.trait.IntegerBlockTrait;
import org.cloudburstmc.server.utils.data.*;

@UtilityClass
public class BlockTraits {


    public static final IntegerBlockTrait AGE = IntegerBlockTrait.from("age", 16);
    public static final EnumBlockTrait<AttachmentType> ATTACHMENT = EnumBlockTrait.of("attachment", AttachmentType.class);
    public static final EnumBlockTrait<BambooLeafSize> BAMBOO_LEAF_SIZE = EnumBlockTrait.of("bamboo_leaf_size", BambooLeafSize.class);
    public static final EnumBlockTrait<BambooStalkThickness> BAMBOO_STALK_THICKNESS = EnumBlockTrait.of("bamboo_stalk_thickness", BambooStalkThickness.class);
    public static final IntegerBlockTrait BITE_COUNTER = IntegerBlockTrait.from("bite_counter", 7);
    public static final IntegerBlockTrait LIGHT_LEVEL = IntegerBlockTrait.from("light_level", 16);
    public static final EnumBlockTrait<FluidType> FLUID_TYPE = EnumBlockTrait.of("fluid_type", FluidType.class);
    public static final EnumBlockTrait<ChemistryTableType> CHEMISTRY_TABLE_TYPE = EnumBlockTrait.of("chemistry_table_type", ChemistryTableType.class);
    public static final EnumBlockTrait<ChiselType> CHISEL_TYPE = EnumBlockTrait.of("chisel_type", ChiselType.class);
    public static final IntegerBlockTrait CLUSTER_COUNT = IntegerBlockTrait.from("cluster_count", 4);
    public static final EnumBlockTrait<DyeColor> COLOR = EnumBlockTrait.of("color", DyeColor.class);
    public static final IntegerBlockTrait COMPOSTER_FILL_LEVEL = IntegerBlockTrait.from("composter_fill_level", 9);
    public static final EnumBlockTrait<DyeColor> CORAL_COLOR = EnumBlockTrait.of("coral_color", DyeColor.class,
            DyeColor.BLUE, DyeColor.PINK, DyeColor.PURPLE, DyeColor.RED, DyeColor.YELLOW);
    public static final EnumBlockTrait<CrackedState> CRACKED_STATE = EnumBlockTrait.of("cracked_state", CrackedState.class);
    public static final EnumBlockTrait<AnvilDamage> DAMAGE = EnumBlockTrait.of("damage", AnvilDamage.class);
    public static final EnumBlockTrait<DirtType> DIRT_TYPE = EnumBlockTrait.of("dirt_type", DirtType.class);
    public static final EnumBlockTrait<DoublePlantType> DOUBLE_PLANT_TYPE = EnumBlockTrait.of("double_plant_type", DoublePlantType.class);
    public static final BooleanBlockTrait DRAG_DOWN = BooleanBlockTrait.of("drag_down");
    public static final BooleanBlockTrait EXPLODE = BooleanBlockTrait.of("explode");
    public static final IntegerBlockTrait FILL_LEVEL = IntegerBlockTrait.from("fill_level", 7);
    public static final EnumBlockTrait<FlowerType> FLOWER_TYPE = EnumBlockTrait.of("flower_type", FlowerType.class);
    public static final IntegerBlockTrait FLUID_LEVEL = IntegerBlockTrait.from("fluid_level", 8);
    public static final IntegerBlockTrait GROWTH = IntegerBlockTrait.from("growth", 8);
    public static final IntegerBlockTrait HEIGHT = IntegerBlockTrait.from("height", 8);
    public static final IntegerBlockTrait HONEY_LEVEL = IntegerBlockTrait.from("honey_level", 8);
    public static final IntegerBlockTrait KELP_AGE = IntegerBlockTrait.from("kelp_age", 26);
    public static final IntegerBlockTrait MOISTURIZED_AMOUNT = IntegerBlockTrait.from("moisturized_amount", 8);
    public static final EnumBlockTrait<PrismarineBlockType> PRISMARINE_BLOCK_TYPE = EnumBlockTrait.of("prismarine_block_type", PrismarineBlockType.class);
    public static final IntegerBlockTrait REDSTONE_SIGNAL = IntegerBlockTrait.from("redstone_signal", 16);
    public static final IntegerBlockTrait REPEATER_DELAY = IntegerBlockTrait.from("repeater_delay", 4);
    public static final IntegerBlockTrait RESPAWN_ANCHOR_CHARGE = IntegerBlockTrait.from("respawn_anchor_charge", 5);
    public static final EnumBlockTrait<SandStoneType> SAND_STONE_TYPE = EnumBlockTrait.of("sand_stone_type", SandStoneType.class);
    public static final EnumBlockTrait<SandType> SAND_TYPE = EnumBlockTrait.of("sand_type", SandType.class);
    public static final EnumBlockTrait<SeaGrassType> SEA_GRASS_TYPE = EnumBlockTrait.of("sea_gras_type", SeaGrassType.class);
    public static final EnumBlockTrait<SpongeType> SPONGE_TYPE = EnumBlockTrait.of("sponge_type", SpongeType.class);
    public static final IntegerBlockTrait STABILITY = IntegerBlockTrait.from("stability", 8);
    public static final EnumBlockTrait<StoneBrickType> STONE_BRICK_TYPE = EnumBlockTrait.of("stone_brick_type", StoneBrickType.class);
    public static final EnumBlockTrait<StoneSlabType> STONE_SLAB_TYPE = EnumBlockTrait.of("stone_slab_type", StoneSlabType.class);
    public static final EnumBlockTrait<StructureBlockType> STRUCTURE_BLOCK_TYPE = EnumBlockTrait.of("structure_block_type", StructureBlockType.class);
    public static final EnumBlockTrait<StructureVoidType> STRUCTURE_VOID_TYPE = EnumBlockTrait.of("structure_void_type", StructureVoidType.class);
    public static final EnumBlockTrait<TallGrassType> TALL_GRASS_TYPE = EnumBlockTrait.of("tall_grass_type", TallGrassType.class);
    public static final EnumBlockTrait<TreeSpecies> TREE_SPECIES = EnumBlockTrait.of("tree_species", TreeSpecies.class);
    public static final IntegerBlockTrait TURTLE_EGG_COUNT = IntegerBlockTrait.from("turtle_egg_count", 1, 4);
    public static final IntegerBlockTrait TWISTING_VINES_AGE = IntegerBlockTrait.from("twisting_vines_age", 26);
    public static final EnumBlockTrait<WallConnectionType> WALL_CONNECTION_NORTH = EnumBlockTrait.of("wall_connection_north", WallConnectionType.class);
    public static final EnumBlockTrait<WallConnectionType> WALL_CONNECTION_EAST = EnumBlockTrait.of("wall_connection_east", WallConnectionType.class);
    public static final EnumBlockTrait<WallConnectionType> WALL_CONNECTION_SOUTH = EnumBlockTrait.of("wall_connection_south", WallConnectionType.class);
    public static final EnumBlockTrait<WallConnectionType> WALL_CONNECTION_WEST = EnumBlockTrait.of("wall_connection_west", WallConnectionType.class);
    public static final IntegerBlockTrait WEEPING_VINES_AGE = IntegerBlockTrait.from("weeping_vines_age", 26);

    public static final BooleanBlockTrait IS_ALLOWED_UNDERWATER = BooleanBlockTrait.of("is_allowed_underwater");
    public static final BooleanBlockTrait IS_ATTACHED = BooleanBlockTrait.of("is_attached");
    public static final BooleanBlockTrait IS_BUTTON_PRESSED = BooleanBlockTrait.of("is_button_pressed");
    public static final BooleanBlockTrait IS_CONDITIONAL = BooleanBlockTrait.of("is_conditional");
    public static final BooleanBlockTrait IS_COVERED = BooleanBlockTrait.of("is_covered");
    public static final BooleanBlockTrait IS_DEAD = BooleanBlockTrait.of("is_dead");
    public static final BooleanBlockTrait IS_DISARMED = BooleanBlockTrait.of("is_disarmed");
    public static final BooleanBlockTrait IS_DOOR_HINGE = BooleanBlockTrait.of("is_door_hinge");
    public static final BooleanBlockTrait IS_EXTINGUISHED = BooleanBlockTrait.of("is_extinguished");
    public static final BooleanBlockTrait IS_FLOWING = BooleanBlockTrait.of("is_flowing");
    public static final BooleanBlockTrait IS_HANGING = BooleanBlockTrait.of("is_hanging");
    public static final BooleanBlockTrait IS_HEAD_PIECE = BooleanBlockTrait.of("is_head_piece");
    public static final BooleanBlockTrait IS_IN_WALL = BooleanBlockTrait.of("is_in_wall");
    public static final BooleanBlockTrait IS_OCCUPIED = BooleanBlockTrait.of("is_occupied");
    public static final BooleanBlockTrait IS_OPEN = BooleanBlockTrait.of("is_open");
    public static final BooleanBlockTrait IS_OUTPUT_LIT = BooleanBlockTrait.of("is_output_lit");
    public static final BooleanBlockTrait IS_OUTPUT_SUBTRACT = BooleanBlockTrait.of("is_output_subtract");
    public static final BooleanBlockTrait IS_PERSISTENT = BooleanBlockTrait.of("is_persistent");
    public static final BooleanBlockTrait IS_POWERED = BooleanBlockTrait.of("is_powered");
    public static final BooleanBlockTrait IS_STRIPPED = BooleanBlockTrait.of("is_stripped");
    public static final BooleanBlockTrait IS_SUSPENDED = BooleanBlockTrait.of("is_suspended");
    public static final BooleanBlockTrait IS_TOGGLED = BooleanBlockTrait.of("is_toggled");
    public static final BooleanBlockTrait IS_TOP_SLOT = BooleanBlockTrait.of("is_top_slot");
    public static final BooleanBlockTrait IS_TRIGGERED = BooleanBlockTrait.of("is_triggered");
    public static final BooleanBlockTrait IS_UPPER_BLOCK = BooleanBlockTrait.of("is_upper_block");
    public static final BooleanBlockTrait IS_UPSIDE_DOWN = BooleanBlockTrait.of("is_upside_down");
    public static final BooleanBlockTrait IS_WALL_POST = BooleanBlockTrait.of("is_wall_post");
    public static final BooleanBlockTrait HAS_AGE = BooleanBlockTrait.of("has_age");
    public static final BooleanBlockTrait HAS_BREWING_STAND_SLOT_A = BooleanBlockTrait.of("has_brewing_stand_slot_a");
    public static final BooleanBlockTrait HAS_BREWING_STAND_SLOT_B = BooleanBlockTrait.of("has_brewing_stand_slot_b");
    public static final BooleanBlockTrait HAS_BREWING_STAND_SLOT_C = BooleanBlockTrait.of("has_brewing_stand_slot_c");
    public static final BooleanBlockTrait HAS_COLOR = BooleanBlockTrait.of("has_color");
    public static final BooleanBlockTrait HAS_END_PORTAL_EYE = BooleanBlockTrait.of("has_end_portal_eye");
    public static final BooleanBlockTrait HAS_INFINIBURN = BooleanBlockTrait.of("has_infiniburn");
    public static final BooleanBlockTrait HAS_NO_DROP = BooleanBlockTrait.of("has_no_drop");
    public static final BooleanBlockTrait HAS_UPDATE = BooleanBlockTrait.of("has_update");
}