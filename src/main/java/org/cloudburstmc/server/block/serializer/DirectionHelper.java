package org.cloudburstmc.server.block.serializer;

import com.google.common.base.Preconditions;
import com.nukkitx.nbt.NbtMapBuilder;
import lombok.experimental.UtilityClass;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.block.BlockTraits;
import org.cloudburstmc.server.math.Direction;
import org.cloudburstmc.server.math.NukkitMath;
import org.cloudburstmc.server.utils.Identifier;

import javax.annotation.Nonnull;
import java.util.*;

import static org.cloudburstmc.server.block.BlockTypes.*;
import static org.cloudburstmc.server.block.serializer.DirectionHelper.SeqType.*;

@UtilityClass
public class DirectionHelper {

    private final Map<SeqType, List<Direction>> faceObjTranslators = new EnumMap<>(SeqType.class);
    private final Map<SeqType, Map<Direction, Byte>> faceMetaTranslators = new EnumMap<>(SeqType.class);

    private final Map<Identifier, SeqType> mapping = new HashMap<>();

    public void init() {
        register(TYPE_1, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST);
        register(TYPE_2, Direction.SOUTH, Direction.WEST, Direction.NORTH, Direction.EAST);
        register(TYPE_3, Direction.EAST, Direction.SOUTH, Direction.WEST, Direction.NORTH);
        register(TYPE_4, Direction.EAST, Direction.WEST, Direction.SOUTH, Direction.NORTH);
        register(TYPE_5, Direction.SOUTH, Direction.NORTH, Direction.EAST, Direction.WEST);
        register(TYPE_6, Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST);

        register(TYPE_7, Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST);
        register(TYPE_8, Direction.DOWN, Direction.UP, Direction.SOUTH, Direction.NORTH, Direction.EAST, Direction.WEST);
        register(TYPE_9, Direction.DOWN, Direction.EAST, Direction.WEST, Direction.SOUTH, Direction.NORTH, Direction.UP);
        register(TYPE_10, Direction.DOWN, Direction.UP, Direction.SOUTH, Direction.WEST, Direction.NORTH, Direction.EAST);
        register(TYPE_11, Direction.EAST, Direction.WEST, Direction.SOUTH, Direction.NORTH, Direction.DOWN, Direction.UP);

        registerDefaultMappings();
    }

    private void registerDefaultMappings() {
        register(TYPE_1,
                END_PORTAL_FRAME,
                BELL
        );

        register(TYPE_2,
                ANVIL,
                BED,
                FENCE_GATE,
                ACACIA_FENCE_GATE,
                NETHER_BRICK_FENCE,
                BIRCH_FENCE_GATE,
                DARK_OAK_FENCE_GATE,
                JUNGLE_FENCE_GATE,
                CRIMSON_FENCE_GATE,
                WARPED_FENCE_GATE,
                POWERED_REPEATER,
                UNPOWERED_REPEATER,
                PUMPKIN,
                CARVED_PUMPKIN,
                LIT_PUMPKIN,
                TRIPWIRE_HOOK
        );

        register(TYPE_3,
                ACACIA_DOOR,
                BIRCH_DOOR,
                DARK_OAK_DOOR,
                IRON_DOOR,
                JUNGLE_DOOR,
                SPRUCE_DOOR,
                WOODEN_DOOR
        );

        register(TYPE_4,
                OAK_STAIRS,
                ACACIA_STAIRS,
                ANDESITE_STAIRS,
                BIRCH_STAIRS,
                BLACKSTONE_STAIRS,
                BRICK_STAIRS,
                CRIMSON_STAIRS,
                DARK_OAK_STAIRS,
                DARK_PRISMARINE_STAIRS,
                DIORITE_STAIRS,
                END_BRICK_STAIRS,
                GRANITE_STAIRS,
                JUNGLE_STAIRS,
                MOSSY_COBBLESTONE_STAIRS,
                MOSSY_STONE_BRICK_STAIRS,
                NETHER_BRICK_STAIRS,
                NORMAL_STONE_STAIRS,
                OAK_STAIRS,
                POLISHED_ANDESITE_STAIRS,
                POLISHED_BLACKSTONE_BRICK_STAIRS,
                POLISHED_BLACKSTONE_STAIRS,
                POLISHED_DIORITE_STAIRS,
                POLISHED_GRANITE_STAIRS,
                PRISMARINE_BRICKS_STAIRS,
                PRISMARINE_STAIRS,
                PURPUR_STAIRS,
                QUARTZ_STAIRS,
                RED_NETHER_BRICK_STAIRS,
                RED_SANDSTONE_STAIRS,
                SANDSTONE_STAIRS,
                SMOOTH_QUARTZ_STAIRS,
                SMOOTH_RED_SANDSTONE_STAIRS,
                SMOOTH_SANDSTONE_STAIRS,
                SPRUCE_STAIRS,
                STONE_BRICK_STAIRS,
                STONE_STAIRS,
                WARPED_STAIRS
        );

        register(TYPE_5,
                TRAPDOOR,
                ACACIA_TRAPDOOR,
                BIRCH_TRAPDOOR,
                DARK_OAK_TRAPDOOR,
                IRON_TRAPDOOR,
                JUNGLE_TRAPDOOR,
                SPRUCE_TRAPDOOR,
                CRIMSON_TRAPDOOR,
                WARPED_TRAPDOOR
        );

        register(TYPE_6,
                COCOA,
                POWERED_COMPARATOR,
                UNPOWERED_COMPARATOR
        );

        register(TYPE_7,
                HOPPER,
                DROPPER,
                DISPENSER,
                END_ROD,
                PISTON,
                STICKY_PISTON,
                PISTON_ARM_COLLISION,
                LADDER,
                WALL_SIGN,
                WALL_BANNER
        );

        register(TYPE_8,
                OBSERVER
        );

        register(TYPE_9,
                SPRUCE_BUTTON,
                JUNGLE_BUTTON,
                DARK_OAK_BUTTON,
                ACACIA_BUTTON,
                BIRCH_BUTTON,
                WOODEN_BUTTON,
                STONE_BUTTON,
                TORCH,
                REDSTONE_TORCH,
                UNLIT_REDSTONE_TORCH
        );

        register(TYPE_10,
                CHEST,
                ENDER_CHEST,
                TRAPPED_CHEST,
                FURNACE,
                LIT_FURNACE,
                BLAST_FURNACE,
                LIT_BLAST_FURNACE
        );

        register(TYPE_11,
                FRAME
        );
    }

    public void register(SeqType type, Identifier... identifiers) {
        for (Identifier identifier : identifiers) {
            mapping.put(identifier, type);
        }
    }

    private void register(SeqType type, Direction... seq) {
        EnumMap<Direction, Byte> map = new EnumMap<>(Direction.class);

        for (byte i = 0; i < seq.length; i++) {
            map.put(seq[i], i);
        }

        faceObjTranslators.put(type, new ArrayList<>(Arrays.asList(seq)));
        faceMetaTranslators.put(type, map);
    }

    public Direction fromMeta(int meta, SeqType type) {
        List<Direction> list = faceObjTranslators.get(type);

        meta = NukkitMath.clamp(meta, 0, list.size() - 1);

        return list.get(meta);
    }

    public short toMeta(@Nonnull Direction direction, SeqType type) {
        Preconditions.checkNotNull(direction);
        return faceMetaTranslators.get(type).get(direction);
    }

    @SuppressWarnings("ConstantConditions")
    public short serialize(@Nonnull NbtMapBuilder builder, @Nonnull BlockState state) {
        SeqType type = mapping.getOrDefault(state.getType(), TYPE_7); //2 is the most common

        Direction direction = state.getTrait(BlockTraits.DIRECTION);

        if (direction == null) {
            direction = state.getTrait(BlockTraits.FACING_DIRECTION);
        }

        if (direction == null) {
            direction = state.getTrait(BlockTraits.TORCH_DIRECTION);
        }

        return toMeta(direction, type);
    }

    @SuppressWarnings("ConstantConditions")
    public void serialize(@Nonnull NbtMapBuilder builder, @Nonnull BlockState state, SeqType type) {
        builder.putInt("direction", toMeta(state.getTrait(BlockTraits.DIRECTION), type));
    }

    public enum SeqType {
        //horizontal
        TYPE_1,
        TYPE_2,
        TYPE_3,
        TYPE_4,
        TYPE_5,
        TYPE_6, //2 reversed

        //omnidirectional
        TYPE_7,
        TYPE_8,
        TYPE_9,
        TYPE_10,
        TYPE_11,
    }
}
