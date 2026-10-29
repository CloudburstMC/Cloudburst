package org.cloudburstmc.server.block.trait;

import org.cloudburstmc.api.block.BlockTraits;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.util.data.RedstoneSide;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;

import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BlockTraitSerializersTest {

    @Test
    @ResourceLock(Resources.LOCALE)
    void redstoneConnectionsUseLocaleIndependentNames() {
        Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            NbtMapBuilder builder = NbtMap.builder();

            BlockTraitSerializers.serialize(builder, BlockTypes.REDSTONE_WIRE,
                    Map.of(BlockTraits.REDSTONE_EAST, RedstoneSide.SIDE), BlockTraits.REDSTONE_EAST);

            assertEquals("side", builder.build().getString("redstone_east"));
        } finally {
            Locale.setDefault(previous);
        }
    }
}
