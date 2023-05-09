package org.cloudburstmc.server.block.serializer;

import com.google.common.base.Preconditions;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;
import org.cloudburstmc.api.block.BlockType;
import org.cloudburstmc.api.block.trait.BlockTrait;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.server.block.serializer.MultiBlockSerializers.MultiBlock;

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

        var states = ((NbtMap) builder.get(TAG_STATES)).toBuilder();
        toRemove.forEach(states::remove);
        builder.putCompound(TAG_STATES, states.build());

    }

    public Identifier getName(NbtMap states, List<String> toRemove) {
        Identifier id = multiBlock.getId(states, toRemove);
        if (id != null) {
            return id;
        }

        if (multiBlock.getDefaultId() != null) {
            return multiBlock.getDefaultId();
        }

        log.info("{}:\n{}", id, states);
        throw new IllegalArgumentException("Invalid block state");
    }
}
