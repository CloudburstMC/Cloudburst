package org.cloudburstmc.server.block.serializer;

import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtMapBuilder;
import lombok.RequiredArgsConstructor;
import org.cloudburstmc.server.block.BlockType;
import org.cloudburstmc.server.block.serializer.MultiBlockSerializers.MultiBlock;
import org.cloudburstmc.server.block.trait.BlockTrait;
import org.cloudburstmc.server.utils.Identifier;

import java.util.Map;

@RequiredArgsConstructor
public class MultiBlockSerializer extends DefaultBlockSerializer {

    private final MultiBlock multiBlock;

    @Override
    public void serialize(NbtMapBuilder builder, BlockType blockType, Map<BlockTrait<?>, Comparable<?>> traits) {
        super.serialize(builder, blockType, traits);

        Identifier name = this.getName((NbtMap) builder.get(TAG_STATES));
        builder.putString(TAG_NAME, name.toString());
    }

    public Identifier getName(NbtMap states) {
        Identifier id = multiBlock.getId(states);
        if (id != null) {
            return id;
        }

        if (multiBlock.getDefaultId() != null) {
            return multiBlock.getDefaultId();
        }

        throw new IllegalArgumentException("Invalid block state");
    }
}
