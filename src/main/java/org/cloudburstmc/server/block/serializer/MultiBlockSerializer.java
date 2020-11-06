package org.cloudburstmc.server.block.serializer;

import com.google.common.base.Preconditions;
import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtMapBuilder;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;
import lombok.val;
import org.cloudburstmc.server.block.BlockType;
import org.cloudburstmc.server.block.serializer.MultiBlockSerializers.MultiBlock;
import org.cloudburstmc.server.block.trait.BlockTrait;
import org.cloudburstmc.server.utils.Identifier;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@Log4j2
@ToString
public class MultiBlockSerializer implements BlockSerializer {

    private final BlockSerializer parent;
    private final MultiBlock multiBlock;

    public MultiBlockSerializer(BlockSerializer parent, MultiBlock multiBlock) {
        Preconditions.checkNotNull(parent, "parent");
        Preconditions.checkNotNull(multiBlock, "multiBlock");
        this.parent = parent;
        this.multiBlock = multiBlock;
    }

    @Override
    public void serialize(NbtMapBuilder builder, BlockType blockType, Map<BlockTrait<?>, Comparable<?>> traits) {
        parent.serialize(builder, blockType, traits);

        List<String> toRemove = new LinkedList<>();

        Identifier name = this.getName((NbtMap) builder.get(TAG_STATES), toRemove);
        builder.putString(TAG_NAME, name.toString());

        val states = ((NbtMap) builder.get("states")).toBuilder();
        toRemove.forEach(states::remove);
        builder.putCompound("states", states.build());

//        if(blockType == BlockTypes.STONE_SLAB) {
//            log.info(traits);
//            log.info(builder);
//        }
    }

    public Identifier getName(NbtMap states, List<String> toRemove) {
        Identifier id = multiBlock.getId(states, toRemove);
        if (id != null) {
            return id;
        }

        if (multiBlock.getDefaultId() != null) {
            return multiBlock.getDefaultId();
        }

        log.info(states);
        throw new IllegalArgumentException("Invalid block state");
    }
}
