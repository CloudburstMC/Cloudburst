package org.cloudburstmc.api.block;

import org.cloudburstmc.api.block.trait.BlockTrait;
import org.cloudburstmc.api.block.trait.BooleanBlockTrait;
import org.cloudburstmc.api.block.trait.EnumBlockTrait;
import org.cloudburstmc.api.block.trait.IntegerBlockTrait;
import org.cloudburstmc.api.registry.BlockRegistry;
import org.cloudburstmc.api.util.Identifier;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BlockTraitAccessTest {

    @Test
    void readsAndChangesBuiltInTraitsThroughApiContracts() {
        BlockState initial = BlockTypes.WHEAT.getDefaultState();
        IntegerBlockTrait growth = BlockTraits.GROWTH;
        int maximum = growth.getRange().getEnd();

        assertTrue(initial.getType().getTraits().contains(growth));
        assertEquals(growth.getDefaultValue(), initial.ensureTrait(growth));
        assertEquals(Integer.class, growth.getValueClass());

        BlockState grown = initial.withTrait(growth, maximum);
        assertEquals(maximum, grown.ensureTrait(growth));
        assertEquals(growth.getDefaultValue(), initial.ensureTrait(growth));
        assertSame(grown, grown.incrementTrait(growth));
        assertSame(initial, grown.withTrait(growth, growth.getDefaultValue().intValue()));
        assertThrows(IllegalArgumentException.class, () -> initial.withTrait(growth, maximum + 1));
    }

    @Test
    void discoversAndUpdatesTraitsWithoutKnowingTheirValueType() {
        BlockState initial = BlockTypes.SMALL_DRIPLEAF_BLOCK.getDefaultState();

        for (BlockTrait<?> trait : initial.getType().getTraits()) {
            BlockState changed = useLastValue(initial, trait);
            assertEquals(trait.getPossibleValues().getLast(), changed.getTraits().get(trait));
        }

        assertThrows(UnsupportedOperationException.class, () -> initial.getTraits().clear());
        assertThrows(UnsupportedOperationException.class, () -> initial.getType().getTraits().clear());
        List<Integer> allowedValues = BlockTraits.GROWTH.getPossibleValues();
        assertThrows(UnsupportedOperationException.class, allowedValues::clear);
    }

    @Test
    void createsPluginTraitsAndCanonicalStatesUsingOnlyApiTypes() {
        BooleanBlockTrait enabled = BooleanBlockTrait.of("test:enabled");
        IntegerBlockTrait charge = IntegerBlockTrait.of("test:charge", 0, 2, 1);
        EnumBlockTrait<Mode> mode = EnumBlockTrait.of("test:mode", Mode.class);
        BlockType type = BlockRegistry.get().createType(Identifier.parse("test:trait_access"), enabled, charge, mode);
        BlockState initial = type.getDefaultState();
        BlockState changed = initial.toggleTrait(enabled).incrementTrait(charge).withTrait(mode, Mode.HIGH);

        assertEquals(12, type.getStates().size());
        assertTrue(changed.ensureTrait(enabled));
        assertEquals(2, changed.ensureTrait(charge));
        assertEquals(Mode.HIGH, changed.ensureTrait(mode));
        assertSame(initial, changed.toggleTrait(enabled).decrementTrait(charge).withTrait(mode, Mode.LOW));

        BooleanBlockTrait sameName = BooleanBlockTrait.of("test:enabled");
        assertFalse(type.getTraits().contains(sameName));
        assertThrows(IllegalArgumentException.class, () -> initial.withTrait(sameName, true));
    }

    private static <T extends Comparable<T>> BlockState useLastValue(BlockState state, BlockTrait<T> trait) {
        return state.withTrait(trait, trait.getPossibleValues().getLast());
    }

    private enum Mode {
        LOW,
        HIGH
    }
}
