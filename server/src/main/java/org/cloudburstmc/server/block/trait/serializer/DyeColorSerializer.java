package org.cloudburstmc.server.block.trait.serializer;

import com.nukkitx.nbt.NbtMapBuilder;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.block.trait.BlockTraitSerializers.TraitSerializer;
import org.cloudburstmc.server.utils.data.DyeColor;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class DyeColorSerializer implements TraitSerializer<DyeColor> {

    @Override
    public Comparable<?> serialize(NbtMapBuilder builder, BlockState state, DyeColor color) {
        if (color == DyeColor.LIGHT_GRAY) {
            return "silver";
        }
        return null;
    }
}
