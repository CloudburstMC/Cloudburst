package org.cloudburstmc.server.block.serializer;

import com.google.common.base.Preconditions;
import lombok.experimental.UtilityClass;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.cloudburstmc.api.block.BlockTraits;
import org.cloudburstmc.api.block.BlockType;
import org.cloudburstmc.api.block.trait.BlockTrait;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.nbt.NbtMapBuilder;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import static org.cloudburstmc.api.block.BlockTypes.*;
import static org.cloudburstmc.server.block.serializer.DirectionHelper.SeqType.*;

@UtilityClass
public class DirectionHelper {

    private final Map<SeqType, Map<Direction, Byte>> faceMetaTranslators = new EnumMap<>(SeqType.class);
    private final Map<BlockType, SeqType> mapping = new HashMap<>();

    public void init() {
        // Horizontal (4-direction) mappings
        register(TYPE_1, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST);
        register(TYPE_2, Direction.SOUTH, Direction.WEST, Direction.NORTH, Direction.EAST);
        register(TYPE_3, Direction.EAST, Direction.WEST, Direction.SOUTH, Direction.NORTH);
        register(TYPE_4, Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST);

        // Omnidirectional (6-direction) mappings
        register(TYPE_5, Direction.DOWN, Direction.UP, Direction.SOUTH, Direction.NORTH, Direction.EAST, Direction.WEST);
        register(TYPE_6, Direction.DOWN, Direction.EAST, Direction.WEST, Direction.SOUTH, Direction.NORTH, Direction.UP);
        register(TYPE_7, Direction.EAST, Direction.WEST, Direction.SOUTH, Direction.NORTH, Direction.DOWN, Direction.UP);
        register(TYPE_8, Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST);

        registerDefaultMappings();
    }

    private void registerDefaultMappings() {
        // TYPE_1: palette key "direction", values 0-3 as N/S/W/E
        register(TYPE_1,
                BEE_NEST,
                BEEHIVE,
                BELL,
                GRINDSTONE,
                LOOM
        );

        // TYPE_2: palette key "direction", values 0-3 as S/W/N/E
        register(TYPE_2,
                BED,
                CHISELED_BOOKSHELF,
                DECORATED_POT,
                TRIPWIRE_HOOK
        );

        // TYPE_3: palette key "direction" (trapdoors) or "weirdo_direction" (stairs), values 0-3 as E/W/S/N
        register(TYPE_3,
                ACACIA_STAIRS,
                ACACIA_TRAPDOOR,
                ANDESITE_STAIRS,
                BAMBOO_MOSAIC_STAIRS,
                BAMBOO_STAIRS,
                BAMBOO_TRAPDOOR,
                BIRCH_STAIRS,
                BIRCH_TRAPDOOR,
                BLACKSTONE_STAIRS,
                BRICK_STAIRS,
                CHERRY_STAIRS,
                CHERRY_TRAPDOOR,
                COBBLED_DEEPSLATE_STAIRS,
                COBBLESTONE_STAIRS,
                COPPER_TRAPDOOR,
                CRIMSON_STAIRS,
                CRIMSON_TRAPDOOR,
                CUT_COPPER_STAIRS,
                DARK_OAK_STAIRS,
                DARK_OAK_TRAPDOOR,
                DARK_PRISMARINE_STAIRS,
                DEEPSLATE_BRICK_STAIRS,
                DEEPSLATE_TILE_STAIRS,
                DIORITE_STAIRS,
                END_BRICK_STAIRS,
                EXPOSED_COPPER_TRAPDOOR,
                EXPOSED_CUT_COPPER_STAIRS,
                GRANITE_STAIRS,
                IRON_TRAPDOOR,
                JUNGLE_STAIRS,
                JUNGLE_TRAPDOOR,
                MANGROVE_STAIRS,
                MANGROVE_TRAPDOOR,
                MOSSY_COBBLESTONE_STAIRS,
                MOSSY_STONE_BRICK_STAIRS,
                MUD_BRICK_STAIRS,
                NETHER_BRICK_STAIRS,
                OAK_STAIRS,
                OAK_TRAPDOOR,
                OXIDIZED_COPPER_TRAPDOOR,
                OXIDIZED_CUT_COPPER_STAIRS,
                PALE_OAK_STAIRS,
                PALE_OAK_TRAPDOOR,
                POLISHED_ANDESITE_STAIRS,
                POLISHED_BLACKSTONE_BRICK_STAIRS,
                POLISHED_BLACKSTONE_STAIRS,
                POLISHED_DEEPSLATE_STAIRS,
                POLISHED_DIORITE_STAIRS,
                POLISHED_GRANITE_STAIRS,
                POLISHED_TUFF_STAIRS,
                PRISMARINE_BRICKS_STAIRS,
                PRISMARINE_STAIRS,
                PURPUR_STAIRS,
                QUARTZ_STAIRS,
                RED_NETHER_BRICK_STAIRS,
                RED_SANDSTONE_STAIRS,
                RESIN_BRICK_STAIRS,
                SANDSTONE_STAIRS,
                SMOOTH_QUARTZ_STAIRS,
                SMOOTH_RED_SANDSTONE_STAIRS,
                SMOOTH_SANDSTONE_STAIRS,
                SPRUCE_STAIRS,
                SPRUCE_TRAPDOOR,
                STONE_BRICK_STAIRS,
                STONE_STAIRS,
                TUFF_BRICK_STAIRS,
                TUFF_STAIRS,
                WARPED_STAIRS,
                WARPED_TRAPDOOR,
                WAXED_COPPER_TRAPDOOR,
                WAXED_CUT_COPPER_STAIRS,
                WAXED_EXPOSED_COPPER_TRAPDOOR,
                WAXED_EXPOSED_CUT_COPPER_STAIRS,
                WAXED_OXIDIZED_COPPER_TRAPDOOR,
                WAXED_OXIDIZED_CUT_COPPER_STAIRS,
                WAXED_WEATHERED_COPPER_TRAPDOOR,
                WAXED_WEATHERED_CUT_COPPER_STAIRS,
                WEATHERED_COPPER_TRAPDOOR,
                WEATHERED_CUT_COPPER_STAIRS
        );

        // TYPE_4: palette key "direction", values 0-3 as N/E/S/W
        register(TYPE_4,
                COCOA
        );

        // TYPE_5: palette key "facing_direction", values 0-5 as D/U/S/N/E/W
        register(TYPE_5,
                END_ROD,
                OBSERVER
        );

        // TYPE_6: palette key "facing_direction", values 0-5 as D/E/W/S/N/U
        register(TYPE_6,
                ACACIA_BUTTON,
                BAMBOO_BUTTON,
                BIRCH_BUTTON,
                CHERRY_BUTTON,
                CRIMSON_BUTTON,
                DARK_OAK_BUTTON,
                JUNGLE_BUTTON,
                MANGROVE_BUTTON,
                OAK_BUTTON,
                PALE_OAK_BUTTON,
                POLISHED_BLACKSTONE_BUTTON,
                SPRUCE_BUTTON,
                STONE_BUTTON,
                WARPED_BUTTON
        );

        // TYPE_7: palette key "facing_direction", values 0-5 as E/W/S/N/D/U
        register(TYPE_7,
                FRAME,
                GLOW_FRAME
        );

        // TYPE_8: palette key "facing_direction", values 0-5 as D/U/N/S/W/E (default for unmapped blocks)
        register(TYPE_8,
                ACACIA_WALL_SIGN,
                BAMBOO_WALL_SIGN,
                BIRCH_WALL_SIGN,
                CHERRY_WALL_SIGN,
                CRIMSON_WALL_SIGN,
                DARKOAK_WALL_SIGN,
                DISPENSER,
                DROPPER,
                HOPPER,
                JUNGLE_WALL_SIGN,
                LADDER,
                MANGROVE_WALL_SIGN,
                OAK_WALL_SIGN,
                PALE_OAK_WALL_SIGN,
                PISTON,
                PISTON_ARM_COLLISION,
                SPRUCE_WALL_SIGN,
                STICKY_PISTON,
                STICKY_PISTON_ARM_COLLISION,
                WALL_BANNER,
                WARPED_WALL_SIGN
        );
    }

    public void register(SeqType type, BlockType... types) {
        for (BlockType blockType : types) {
            mapping.put(blockType, type);
        }
    }

    private void register(SeqType type, Direction... seq) {
        EnumMap<Direction, Byte> map = new EnumMap<>(Direction.class);

        for (byte i = 0; i < seq.length; i++) {
            map.put(seq[i], i);
        }

        faceMetaTranslators.put(type, map);
    }

    public short toMeta(@NonNull Direction direction, SeqType type) {
        Preconditions.checkNotNull(direction);
        return faceMetaTranslators.get(type).get(direction);
    }

    @SuppressWarnings("ConstantConditions")
    public int serialize(@NonNull NbtMapBuilder builder, @NonNull BlockType blockType, @NonNull Map<BlockTrait<?>, Comparable<?>> traits) {
        SeqType type = mapping.getOrDefault(blockType, TYPE_8);
        Direction direction = (Direction) traits.get(BlockTraits.DIRECTION);

        if (direction == null) {
            direction = (Direction) traits.get(BlockTraits.FACING_DIRECTION);
        }

        if (direction == null) {
            direction = (Direction) traits.get(BlockTraits.TORCH_DIRECTION);
        }

        if (direction == null) {
            direction = (Direction) traits.get(BlockTraits.BLOCK_FACE);
        }

        return toMeta(direction, type);
    }

    public enum SeqType {
        // Horizontal (4-direction)
        TYPE_1,
        TYPE_2,
        TYPE_3,
        TYPE_4,

        // Omnidirectional (6-direction)
        TYPE_5,
        TYPE_6,
        TYPE_7,
        TYPE_8,
    }
}
