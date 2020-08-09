package org.cloudburstmc.server.block.serializer;

import com.google.common.collect.ImmutableMap;
import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtMapBuilder;
import lombok.RequiredArgsConstructor;
import org.cloudburstmc.server.block.BlockState;

@RequiredArgsConstructor
public class MultiBlockSerializer extends DefaultBlockSerializer {

    private final ImmutableMap<String, String> nameMap;

    @Override
    public void serialize(NbtMapBuilder builder, BlockState state) {
        super.serialize(builder, state);

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
