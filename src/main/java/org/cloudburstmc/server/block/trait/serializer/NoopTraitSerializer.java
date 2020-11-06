package org.cloudburstmc.server.block.trait.serializer;

import com.nukkitx.nbt.NbtMapBuilder;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.cloudburstmc.server.block.BlockType;
import org.cloudburstmc.server.block.trait.BlockTraitSerializers.TraitSerializer;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Map;

@SuppressWarnings("ALL")
@ParametersAreNonnullByDefault
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class NoopTraitSerializer implements TraitSerializer {

    public static final NoopTraitSerializer INSTANCE = new NoopTraitSerializer();

    @Override
    public Comparable<?> serialize(NbtMapBuilder builder, BlockType type, Map traits, Comparable comparable) {
        return null;
    }
}
