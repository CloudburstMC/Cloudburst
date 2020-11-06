package org.cloudburstmc.server.block.serializer;

import com.nukkitx.nbt.NbtMapBuilder;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.cloudburstmc.server.block.BlockTraits;
import org.cloudburstmc.server.block.BlockType;
import org.cloudburstmc.server.block.trait.BlockTrait;
import org.cloudburstmc.server.utils.data.TreeSpecies;

import java.util.HashMap;
import java.util.Map;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class DeprecatedSerializer implements BlockSerializer {

    public static final DeprecatedSerializer INSTANCE = new DeprecatedSerializer();

    @Override
    public void serialize(NbtMapBuilder builder, BlockType blockType, Map<BlockTrait<?>, Comparable<?>> traits) {
        boolean stripped = (Boolean) traits.get(BlockTraits.IS_STRIPPED);
        TreeSpecies type = (TreeSpecies) traits.get(BlockTraits.TREE_SPECIES);

        if (!stripped || (type != TreeSpecies.CRIMSON && type != TreeSpecies.WARPED)) {
            traits = new HashMap<>(traits);
            traits.remove(BlockTraits.DEPRECATED);
        }

        DefaultBlockSerializer.INSTANCE.serialize(builder, blockType, traits);
    }
}
