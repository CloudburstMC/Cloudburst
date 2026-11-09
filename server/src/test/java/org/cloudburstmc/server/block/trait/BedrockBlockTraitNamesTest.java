package org.cloudburstmc.server.block.trait;

import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockTraits;
import org.cloudburstmc.api.block.trait.BooleanBlockTrait;
import org.cloudburstmc.api.registry.BlockRegistry;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BedrockBlockTraitNamesTest {

    @Test
    void builtInNamesRemainAtTheBedrockBoundary() {
        assertEquals("pillar_axis", BedrockBlockTraitNames.name(BlockTraits.AXIS));
        assertEquals("open_bit", BedrockBlockTraitNames.name(BlockTraits.IS_OPEN));
        assertEquals("minecraft:cardinal_direction", BedrockBlockTraitNames.name(BlockTraits.CARDINAL_DIRECTION));
        assertEquals("liquid_depth", BedrockBlockTraitNames.name(BlockTraits.LIQUID_DEPTH));
    }

    @Test
    void customNamespacesPreventUnrelatedTraitNamesFromColliding() {
        BooleanBlockTrait first = BooleanBlockTrait.of("first:mode");
        BooleanBlockTrait second = BooleanBlockTrait.of("second:mode", true);
        BlockState state = BlockRegistry.get().createType(Identifier.parse("test:namespaces"), first, second)
                .getDefaultState();
        BlockTraitSerializers.init();
        NbtMapBuilder builder = NbtMap.builder();
        BlockTraitSerializers.serialize(builder, state.getType(), state.getTraits(), first);
        BlockTraitSerializers.serialize(builder, state.getType(), state.getTraits(), second);

        NbtMap encoded = builder.build();
        assertEquals(2, encoded.size());
        assertFalse(encoded.getBoolean("first:mode"));
        assertTrue(encoded.getBoolean("second:mode"));
    }
}
