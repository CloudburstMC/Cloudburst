package org.cloudburstmc.server.block.serializer;

import com.nukkitx.nbt.NbtMapBuilder;
import lombok.experimental.UtilityClass;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.block.BlockTraits;
import org.cloudburstmc.server.math.BlockFace;
import org.cloudburstmc.server.math.NukkitMath;
import org.cloudburstmc.server.math.SimpleDirection;

import javax.annotation.Nonnull;
import java.util.*;

@UtilityClass
public class DirectionHelper {

    private final Map<SeqType, List<SimpleDirection>> objTranslators = new EnumMap<>(SeqType.class);
    private final Map<SeqType, Map<SimpleDirection, Byte>> metaTranslators = new EnumMap<>(SeqType.class);

    private final Map<SeqType, List<BlockFace>> faceObjTranslators = new EnumMap<>(SeqType.class);
    private final Map<SeqType, Map<BlockFace, Byte>> faceMetaTranslators = new EnumMap<>(SeqType.class);

    static {
        register(SeqType.TYPE_1, SimpleDirection.NORTH, SimpleDirection.SOUTH, SimpleDirection.WEST, SimpleDirection.EAST);
        register(SeqType.TYPE_2, SimpleDirection.SOUTH, SimpleDirection.WEST, SimpleDirection.NORTH, SimpleDirection.EAST);
        register(SeqType.TYPE_3, SimpleDirection.EAST, SimpleDirection.SOUTH, SimpleDirection.WEST, SimpleDirection.NORTH);
        register(SeqType.TYPE_4, SimpleDirection.EAST, SimpleDirection.WEST, SimpleDirection.SOUTH, SimpleDirection.NORTH);
        register(SeqType.TYPE_5, SimpleDirection.SOUTH, SimpleDirection.NORTH, SimpleDirection.EAST, SimpleDirection.WEST);
        register(SeqType.TYPE_6, SimpleDirection.NORTH, SimpleDirection.EAST, SimpleDirection.SOUTH, SimpleDirection.WEST);

        register(SeqType.TYPE_1, BlockFace.DOWN, BlockFace.UP, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.WEST, BlockFace.EAST);
        register(SeqType.TYPE_2, BlockFace.DOWN, BlockFace.UP, BlockFace.SOUTH, BlockFace.NORTH, BlockFace.EAST, BlockFace.WEST);
        register(SeqType.TYPE_3, BlockFace.DOWN, BlockFace.EAST, BlockFace.WEST, BlockFace.SOUTH, BlockFace.NORTH, BlockFace.UP);
    }

    private void register(SeqType type, SimpleDirection... seq) {
        EnumMap<SimpleDirection, Byte> map = new EnumMap<>(SimpleDirection.class);

        for (byte i = 0; i < seq.length; i++) {
            map.put(seq[i], i);
        }

        objTranslators.put(type, new ArrayList<>(Arrays.asList(seq)));
        metaTranslators.put(type, map);
    }

    private void register(SeqType type, BlockFace... seq) {
        EnumMap<BlockFace, Byte> map = new EnumMap<>(BlockFace.class);

        for (byte i = 0; i < seq.length; i++) {
            map.put(seq[i], i);
        }

        faceObjTranslators.put(type, new ArrayList<>(Arrays.asList(seq)));
        faceMetaTranslators.put(type, map);
    }

    public SimpleDirection fromMeta(int meta, SeqType type) {
        List<SimpleDirection> list = objTranslators.get(type);

        meta = NukkitMath.clamp(meta, 0, list.size() - 1);

        return list.get(meta);
    }

    public BlockFace faceFromMeta(int meta, SeqType type) {
        List<BlockFace> list = faceObjTranslators.get(type);

        meta = NukkitMath.clamp(meta, 0, list.size() - 1);

        return list.get(meta);
    }

    @SuppressWarnings("ConstantConditions")
    public void serializeSimple(@Nonnull NbtMapBuilder builder, @Nonnull BlockState state, SeqType type) {
        builder.putInt("direction", toMeta(state.getTrait(BlockTraits.DIRECTION), type));
    }

    public short toMeta(@Nonnull SimpleDirection direction, SeqType type) {
        return metaTranslators.get(type).get(direction);
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
        TYPE_6 //2 reversed
    }
}
