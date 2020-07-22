package org.cloudburstmc.server.block.serializer;

import com.nukkitx.nbt.NbtMapBuilder;
import lombok.experimental.UtilityClass;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.block.BlockTraits;
import org.cloudburstmc.server.math.BlockFace;
import org.cloudburstmc.server.math.NukkitMath;

import javax.annotation.Nonnull;
import java.util.*;

@UtilityClass
public class DirectionHelper {

    private final Map<SeqType, List<BlockFace>> faceObjTranslators = new EnumMap<>(SeqType.class);
    private final Map<SeqType, Map<BlockFace, Byte>> faceMetaTranslators = new EnumMap<>(SeqType.class);

    static {
        register(SeqType.TYPE_1, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.WEST, BlockFace.EAST);
        register(SeqType.TYPE_2, BlockFace.SOUTH, BlockFace.WEST, BlockFace.NORTH, BlockFace.EAST);
        register(SeqType.TYPE_3, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST, BlockFace.NORTH);
        register(SeqType.TYPE_4, BlockFace.EAST, BlockFace.WEST, BlockFace.SOUTH, BlockFace.NORTH);
        register(SeqType.TYPE_5, BlockFace.SOUTH, BlockFace.NORTH, BlockFace.EAST, BlockFace.WEST);
        register(SeqType.TYPE_6, BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST);

        register(SeqType.TYPE_7, BlockFace.DOWN, BlockFace.UP, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.WEST, BlockFace.EAST);
        register(SeqType.TYPE_8, BlockFace.DOWN, BlockFace.UP, BlockFace.SOUTH, BlockFace.NORTH, BlockFace.EAST, BlockFace.WEST);
        register(SeqType.TYPE_9, BlockFace.DOWN, BlockFace.EAST, BlockFace.WEST, BlockFace.SOUTH, BlockFace.NORTH, BlockFace.UP);
    }

    private void register(SeqType type, BlockFace... seq) {
        EnumMap<BlockFace, Byte> map = new EnumMap<>(BlockFace.class);

        for (byte i = 0; i < seq.length; i++) {
            map.put(seq[i], i);
        }

        faceObjTranslators.put(type, new ArrayList<>(Arrays.asList(seq)));
        faceMetaTranslators.put(type, map);
    }

    public BlockFace fromMeta(int meta, SeqType type) {
        List<BlockFace> list = faceObjTranslators.get(type);

        meta = NukkitMath.clamp(meta, 0, list.size() - 1);

        return list.get(meta);
    }

    @SuppressWarnings("ConstantConditions")
    public void serializeSimple(@Nonnull NbtMapBuilder builder, @Nonnull BlockState state, SeqType type) {
        builder.putInt("direction", toMeta(state.getTrait(BlockTraits.DIRECTION), type));
    }

    public short toMeta(@Nonnull BlockFace direction, SeqType type) {
        return faceMetaTranslators.get(type).get(direction);
    }

    public enum SeqType {
        TYPE_1,
        TYPE_2,
        TYPE_3,
        TYPE_4,
        TYPE_5,
        TYPE_6, //2 reversed
        TYPE_7,
        TYPE_8,
        TYPE_9
    }
}
