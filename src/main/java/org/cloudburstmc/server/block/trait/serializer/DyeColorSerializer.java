package org.cloudburstmc.server.block.trait.serializer;

import com.nukkitx.nbt.NbtMapBuilder;
import org.cloudburstmc.api.block.BlockType;
import org.cloudburstmc.api.util.data.DyeColor;
import org.cloudburstmc.server.block.trait.BlockTrait;
import org.cloudburstmc.server.block.trait.BlockTraitSerializers.TraitSerializer;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Map;

@ParametersAreNonnullByDefault
public class DyeColorSerializer implements TraitSerializer<DyeColor> {

    @Override
    public Comparable<?> serialize(NbtMapBuilder builder, BlockType type, Map<BlockTrait<?>, Comparable<?>> traits, DyeColor color) {
        if (color == DyeColor.LIGHT_GRAY) {
            return "silver";
        }
        return null;
    }
}
