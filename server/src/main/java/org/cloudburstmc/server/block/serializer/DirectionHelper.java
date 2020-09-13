package org.cloudburstmc.server.block.serializer;

import com.google.common.base.Preconditions;
import com.nukkitx.nbt.NbtMapBuilder;
import lombok.experimental.UtilityClass;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.block.BlockTraits;
import org.cloudburstmc.server.block.BlockType;
import org.cloudburstmc.server.block.trait.BlockTrait;
import org.cloudburstmc.server.math.Direction;
import org.cloudburstmc.server.math.NukkitMath;

import javax.annotation.Nonnull;
import java.util.*;

import static org.cloudburstmc.server.block.BlockTypes.*;
import static org.cloudburstmc.server.block.serializer.DirectionHelper.SeqType.*;

@UtilityClass
public class DirectionHelper {

    private final Map<SeqType, List<Direction>> faceObjTranslators = new EnumMap<>(SeqType.class);
    private final Map<SeqType, Map<Direction, Byte>> faceMetaTranslators = new EnumMap<>(SeqType.class);

    private final Map<BlockType, SeqType> mapping = new HashMap<>();

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
        register(TYPE_10, Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST);
        register(TYPE_11, Direction.EAST, Direction.WEST, Direction.SOUTH, Direction.NORTH, Direction.DOWN, Direction.UP);

        registerDefaultMappings();
    }

    private void registerDefaultMappings() {
        register(TYPE_1,
                END_PORTAL_FRAME,
                //TODO: check below
                BELL,
                CHEMISTRY_TABLE,
                GRINDSTONE,
                CAMPFIRE,
                CORAL_FAN_HANG,
                LECTERN,
                LOOM,
                BEEHIVE,
                BEE_NEST
        );

        register(TYPE_2,
                ANVIL,
                BED,
                FENCE_GATE,
                NETHER_BRICK_FENCE,
                REPEATER,
                UNPOWERED_REPEATER,
                PUMPKIN,
                CARVED_PUMPKIN,
                LIT_PUMPKIN,
                TRIPWIRE_HOOK,
                POWERED_COMPARATOR,
                UNPOWERED_COMPARATOR
        );

        register(TYPE_3,
                IRON_DOOR,
                WOODEN_DOOR
        );

        register(TYPE_4,
                BRICK_STAIRS,
                DARK_PRISMARINE_STAIRS,
                NETHER_BRICK_STAIRS,
                PRISMARINE_BRICKS_STAIRS,
                PRISMARINE_STAIRS,
                PURPUR_STAIRS,
                QUARTZ_STAIRS,
                RED_SANDSTONE_STAIRS,
                SANDSTONE_STAIRS,
                STONE_BRICK_STAIRS,
                STONE_STAIRS,
                TRAPDOOR,
                IRON_TRAPDOOR
        );

        register(TYPE_6,
                COCOA
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
                WALL_BANNER,
                CHEST,
                ENDER_CHEST,
                TRAPPED_CHEST,
                FURNACE,
                LIT_FURNACE,
                BLAST_FURNACE
        );

        register(TYPE_8,
                OBSERVER
        );

        register(TYPE_9,
                WOODEN_BUTTON,
                STONE_BUTTON,
                TORCH,
                REDSTONE_TORCH
        );

        register(TYPE_11,
                FRAME
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
    public int serialize(@Nonnull NbtMapBuilder builder, @Nonnull BlockType blockType, @Nonnull Map<BlockTrait<?>, Comparable<?>> traits) {
        SeqType type = mapping.getOrDefault(blockType, TYPE_7); //2 is the most common

        Direction direction = (Direction) traits.get(BlockTraits.DIRECTION);

        if (direction == null) {
            direction = (Direction) traits.get(BlockTraits.FACING_DIRECTION);
        }

        if (direction == null) {
            direction = (Direction) traits.get(BlockTraits.TORCH_DIRECTION);
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
