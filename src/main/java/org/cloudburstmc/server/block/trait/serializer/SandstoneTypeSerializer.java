package org.cloudburstmc.server.block.trait.serializer;

import com.nukkitx.nbt.NbtMapBuilder;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.block.trait.BlockTraitSerializers.TraitSerializer;
import org.cloudburstmc.server.utils.data.SandStoneType;

public class SandstoneTypeSerializer implements TraitSerializer<SandStoneType> {

    @Override
    public Comparable<?> serialize(NbtMapBuilder builder, BlockState state, SandStoneType sandStoneType) {
        if (sandStoneType == SandStoneType.HIEROGLYPHS) {
            return "heiroglyphs"; //wtf
        }

        return null;
    }
}
