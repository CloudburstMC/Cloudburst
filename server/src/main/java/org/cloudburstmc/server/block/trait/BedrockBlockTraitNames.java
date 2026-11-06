package org.cloudburstmc.server.block.trait;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.block.trait.BlockTrait;
import org.cloudburstmc.api.util.Identifier;

import java.util.Map;

import static java.util.Map.entry;
import static org.cloudburstmc.api.block.BlockTraits.*;

/**
 * Bedrock names that differ from public trait identifiers.
 */
@UtilityClass
public class BedrockBlockTraitNames {

    private static final Map<BlockTrait<?>, String> NAMES = Map.ofEntries(
            entry(AXIS, "pillar_axis"),
            entry(BLOCK_FACE, "minecraft:block_face"),
            entry(CARDINAL_DIRECTION, "minecraft:cardinal_direction"),
            entry(CAULDRON_TYPE, "cauldron_liquid"),
            entry(DRIPLEAF_TILT, "big_dripleaf_tilt"),
            entry(SIGN_DIRECTION, "ground_sign_direction"),
            entry(SIMPLE_RAIL_DIRECTION, "rail_direction"),
            entry(STAIR_SHAPE, "minecraft:corner"),
            entry(TORCH_DIRECTION, "torch_facing_direction"),
            entry(WALL_CONNECTION_EAST, "wall_connection_type_east"),
            entry(WALL_CONNECTION_NORTH, "wall_connection_type_north"),
            entry(WALL_CONNECTION_SOUTH, "wall_connection_type_south"),
            entry(WALL_CONNECTION_WEST, "wall_connection_type_west"),
            entry(CHALKBOARD_DIRECTION, "direction"),
            entry(CHEMISTRY_TABLE_DIRECTION, "direction"),
            entry(MULTI_FACE_DIRECTION, "multi_face_direction_bits"),
            entry(SHELF_MUSHROOM_GROWTH, "growth"),
            entry(CONNECTION_DOWN, "minecraft:connection_down"),
            entry(CONNECTION_EAST, "minecraft:connection_east"),
            entry(CONNECTION_NORTH, "minecraft:connection_north"),
            entry(CONNECTION_SOUTH, "minecraft:connection_south"),
            entry(CONNECTION_UP, "minecraft:connection_up"),
            entry(CONNECTION_WEST, "minecraft:connection_west"),
            entry(CRAFTER_CRAFTING, "crafting"),
            entry(CRAFTER_TRIGGERED, "triggered_bit"),
            entry(DRIPLEAF_HEAD, "big_dripleaf_head"),
            entry(EXPLODE, "explode_bit"),
            entry(HAS_AGE, "age_bit"),
            entry(HAS_END_PORTAL_EYE, "end_portal_eye_bit"),
            entry(HAS_INFINIBURN, "infiniburn_bit"),
            entry(HAS_MAP, "item_frame_map_bit"),
            entry(HAS_PHOTO, "item_frame_photo_bit"),
            entry(HAS_POST, "wall_post_bit"),
            entry(HAS_STABILITY_CHECK, "stability_check"),
            entry(HAS_UPDATE, "update_bit"),
            entry(IS_ACTIVE, "active"),
            entry(IS_ATTACHED, "attached_bit"),
            entry(IS_BLOOMING, "bloom"),
            entry(IS_BREWING_A, "brewing_stand_slot_a_bit"),
            entry(IS_BREWING_B, "brewing_stand_slot_b_bit"),
            entry(IS_BREWING_C, "brewing_stand_slot_c_bit"),
            entry(IS_BUTTON_PRESSED, "button_pressed_bit"),
            entry(IS_CONDITIONAL, "conditional_bit"),
            entry(IS_COVERED, "covered_bit"),
            entry(IS_DEAD, "dead_bit"),
            entry(IS_DISARMED, "disarmed_bit"),
            entry(IS_DOOR_HINGE, "door_hinge_bit"),
            entry(IS_EXTINGUISHED, "extinguished"),
            entry(IS_HANGING, "hanging"),
            entry(IS_HEAD_PIECE, "head_piece_bit"),
            entry(IS_IN_WALL, "in_wall_bit"),
            entry(IS_LIT, "lit"),
            entry(IS_OCCUPIED, "occupied_bit"),
            entry(IS_OMINOUS, "ominous"),
            entry(IS_OPEN, "open_bit"),
            entry(IS_OUTPUT_LIT, "output_lit_bit"),
            entry(IS_OUTPUT_SUBTRACT, "output_subtract_bit"),
            entry(IS_PERSISTENT, "persistent_bit"),
            entry(IS_POWERED, "powered_bit"),
            entry(IS_SUSPENDED, "suspended_bit"),
            entry(IS_TOGGLED, "toggle_bit"),
            entry(IS_TRIGGERED, "triggered_bit"),
            entry(IS_UPPER_BLOCK, "upper_block_bit"),
            entry(IS_UPSIDE_DOWN, "upside_down_bit"));

    /**
     * @param trait the public trait
     * @return its serialized property name, retaining the namespace for custom traits
     */
    public static String name(BlockTrait<?> trait) {
        String mapped = NAMES.get(trait);
        if (mapped != null) {
            return mapped;
        }

        Identifier id = trait.getName();
        return id.getNamespace().equals("minecraft") ? id.getName() : id.toString();
    }
}
