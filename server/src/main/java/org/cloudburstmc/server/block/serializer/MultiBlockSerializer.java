package org.cloudburstmc.server.block.serializer;

import com.google.common.collect.ImmutableMap;
import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtMapBuilder;
import lombok.RequiredArgsConstructor;
import org.cloudburstmc.server.block.BlockType;
import org.cloudburstmc.server.block.trait.BlockTrait;

import java.util.Map;

@RequiredArgsConstructor
public class MultiBlockSerializer extends DefaultBlockSerializer {

    private final ImmutableMap<String, String> nameMap;

    @Override
    public void serialize(NbtMapBuilder builder, BlockType blockType, Map<BlockTrait<?>, Comparable<?>> traits) {
        super.serialize(builder, blockType, traits);

        String name = this.getName((NbtMap) builder.get(TAG_STATES));
        builder.putString(TAG_NAME, name);
    }

    public String getName(NbtMap states) {
        for (String key : states.keySet()) {
            String name = this.nameMap.get(key);
            if (name != null) {
                return name;
            }
        }
        throw new IllegalArgumentException("Invalid block state");
    }
}
