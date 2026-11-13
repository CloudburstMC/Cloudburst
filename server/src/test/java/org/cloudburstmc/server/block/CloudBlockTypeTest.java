package org.cloudburstmc.server.block;

import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockType;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.block.LiquidReaction;
import org.cloudburstmc.api.block.trait.BlockTrait;
import org.cloudburstmc.api.block.trait.BooleanBlockTrait;
import org.cloudburstmc.api.block.trait.IntegerBlockTrait;
import org.cloudburstmc.api.registry.BlockRegistry;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.server.level.collision.CloudVoxelShapes;
import org.junit.jupiter.api.Test;

import java.awt.*;

import static org.junit.jupiter.api.Assertions.*;

class CloudBlockTypeTest {

    @Test
    void builtInCatalogUsesCanonicalRegistryEntries() {
        BlockRegistry registry = BlockRegistry.get();
        assertEquals(BlockTypes.values(), registry.values());

        for (BlockType type : BlockTypes.values()) {
            assertSame(type, registry.getOrThrow(type.getId()));
            assertEquals(type.getId(), registry.getId(type));
        }

        BlockType unregistered = registry.createType(BlockTypes.STONE.getId());
        assertNotSame(BlockTypes.STONE, unregistered);
        assertThrows(IllegalArgumentException.class, () -> registry.getId(unregistered));
    }

    @Test
    void factoryPreservesDefaultsAndCanonicalTraitTransitions() {
        IntegerBlockTrait age = IntegerBlockTrait.of("test_age", 0, 3, 2);
        BooleanBlockTrait active = BooleanBlockTrait.of("test_active", false);
        BlockTrait<?>[] traits = {age, active};
        BlockType type = BlockRegistry.get().createType(Identifier.parse("test:canonical_states"), traits);
        traits[0] = active;
        BlockState initial = type.getDefaultState();

        assertEquals(8, type.getStates().size());
        assertEquals(2, initial.ensureTrait(age));
        assertSame(initial, initial.withTrait(age, 0).withTrait(age, 2));
        assertSame(initial, initial.toggleTrait(active).toggleTrait(active));
        assertThrows(UnsupportedOperationException.class, () -> type.getStates().clear());
        assertThrows(UnsupportedOperationException.class, () -> initial.getTraits().clear());
        assertThrows(IllegalArgumentException.class, () -> BlockRegistry.get().createType(Identifier.parse("test:duplicate_traits"), age, age));
    }

    @Test
    void physicalPropertiesBindOnce() {
        BlockType type = BlockRegistry.get().createType(Identifier.parse("test:properties"));
        BlockState state = type.getDefaultState();
        BlockStateProperties properties = new BlockStateProperties(
                CloudVoxelShapes.block(), CloudVoxelShapes.empty(), 2, 4, 0.6f, 0, 1,
                0, 0, 15, 0, true, false, Color.WHITE, false, LiquidReaction.BROKEN);

        assertThrows(IllegalStateException.class, state::getHardness);
        CloudBlockState.require(state).bindProperties(properties);

        assertEquals(2, type.getHardness());
        assertEquals(4, state.getExplosionResistance());
        assertSame(properties.outlineShape(), state.getOutlineShape());
        assertThrows(IllegalStateException.class, () -> CloudBlockState.require(state).bindProperties(properties));
    }
}
