package org.cloudburstmc.api.block;

import org.cloudburstmc.api.block.trait.BlockTrait;
import org.cloudburstmc.api.block.trait.BooleanBlockTrait;
import org.cloudburstmc.api.block.trait.EnumBlockTrait;
import org.cloudburstmc.api.block.trait.IntegerBlockTrait;
import org.cloudburstmc.api.registry.BlockRegistry;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.api.util.IntRange;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class BlockTraitValidationTest {

    @Test
    void enumDefinitionsRejectEmptyDomainsDuplicatesAndInvalidDefaults() {
        assertThrows(IllegalArgumentException.class, () -> EnumBlockTrait.of("test:empty", Empty.class));
        assertThrows(IllegalArgumentException.class, () -> EnumBlockTrait.of("test:empty", Mode.class, new Mode[0]));
        assertThrows(IllegalArgumentException.class, () -> EnumBlockTrait.of("test:empty", Mode.class, Set.of(), Mode.OPEN));
        assertThrows(IllegalArgumentException.class, () -> EnumBlockTrait.of("test:duplicate", Mode.class, Mode.OPEN, Mode.OPEN));
        assertThrows(IllegalArgumentException.class, () -> EnumBlockTrait.of("test:default", Mode.class, Set.of(Mode.OPEN), Mode.CLOSED));
        assertThrows(IllegalArgumentException.class, () -> EnumBlockTrait.of("test:ambiguous", Ambiguous.class));
    }

    @Test
    void enumDomainsAreImmutableAndParsingNeverSubstitutesTheDefault() {
        Mode[] values = {Mode.CLOSED, Mode.OPEN};
        EnumBlockTrait<Mode> trait = EnumBlockTrait.of("test:mode", Mode.class, values);
        values[0] = Mode.EXTRA;

        assertEquals(List.of(Mode.CLOSED, Mode.OPEN), trait.getPossibleValues());
        assertEquals(Mode.CLOSED, trait.getDefaultValue());
        assertEquals(Optional.of(Mode.OPEN), trait.parseValue("Open"));
        assertTrue(trait.parseValue("typo").isEmpty());
        assertTrue(trait.parseValue("EXTRA").isEmpty());
        assertTrue(trait.testValue(Mode.OPEN));
        assertTrue(trait.testValue(Mode.CLOSED));
        assertFalse(trait.testValue(Mode.EXTRA));
        assertFalse(trait.testValue(Ambiguous.VALUE));
        assertFalse(trait.testValue("OPEN"));
        assertFalse(trait.testValue(new Object()));
        assertFalse(trait.testValue(null));
        // Mutation is intentional to verify the immutable domain contract.
        //noinspection DataFlowIssue
        assertThrows(UnsupportedOperationException.class, () -> trait.getPossibleValues().clear());
    }

    @Test
    void booleansRejectInvalidTextInsteadOfTreatingItAsFalse() {
        BooleanBlockTrait trait = BooleanBlockTrait.of("test:active", true);
        assertEquals(Optional.of(true), trait.parseValue("TRUE"));
        assertEquals(Optional.of(false), trait.parseValue("False"));
        assertTrue(trait.parseValue("invalid").isEmpty());
        assertTrue(trait.parseValue("0").isEmpty());
        assertTrue(trait.parseValue(" true ").isEmpty());
        assertTrue(trait.testValue(true));
        assertTrue(trait.testValue(false));
        assertFalse(trait.testValue("true"));
        assertFalse(trait.testValue(new Object()));
        assertFalse(trait.testValue(null));
    }

    @Test
    void integerDefaultsAndParsersRespectTheInclusiveDomain() {
        IntegerBlockTrait trait = IntegerBlockTrait.of("test:positive", 10, 12);
        assertEquals(10, trait.getDefaultValue());
        assertEquals(List.of(10, 11, 12), trait.getPossibleValues());
        assertEquals(Optional.of(11), trait.parseValue("11"));
        assertTrue(trait.parseValue("9").isEmpty());
        assertTrue(trait.parseValue("13").isEmpty());
        assertTrue(trait.parseValue("2147483648").isEmpty());
        assertTrue(trait.parseValue("11.0").isEmpty());
        assertFalse(trait.testValue(11L));
        assertFalse(trait.testValue("11"));
        assertFalse(trait.testValue(null));

        IntegerBlockTrait singleton = IntegerBlockTrait.of("test:one", 4, 4);
        assertEquals(List.of(4), singleton.getPossibleValues());
        assertEquals(List.of(0), IntegerBlockTrait.of("test:length_one", 1).getPossibleValues());
        assertThrows(IllegalArgumentException.class, () -> IntegerBlockTrait.of("test:bad", 0));
        assertThrows(IllegalArgumentException.class, () -> IntegerBlockTrait.of("test:bad", 3, 2));
        assertThrows(IllegalArgumentException.class, () -> IntegerBlockTrait.of("test:bad", 3, 5, 0));
        assertThrows(IllegalArgumentException.class, () -> IntegerBlockTrait.of("test:bad", Integer.MIN_VALUE, Integer.MAX_VALUE));
    }

    @Test
    void integerStepsDoNotOverflowAtEitherBoundary() {
        IntegerBlockTrait high = IntegerBlockTrait.of("test:high", Integer.MAX_VALUE - 1, Integer.MAX_VALUE, Integer.MAX_VALUE);
        BlockState maximum = BlockRegistry.get().createType(Identifier.parse("test:high_state"), high).getDefaultState();
        assertSame(maximum, maximum.incrementTrait(high));
        assertEquals(Integer.MAX_VALUE - 1, maximum.decrementTrait(high).ensureTrait(high));
        assertSame(maximum, maximum.decrementTrait(high).incrementTrait(high));

        IntegerBlockTrait low = IntegerBlockTrait.of("test:low", Integer.MIN_VALUE, Integer.MIN_VALUE + 1);
        BlockState minimum = BlockRegistry.get().createType(Identifier.parse("test:low_state"), low).getDefaultState();
        assertSame(minimum, minimum.decrementTrait(low));
        assertEquals(Integer.MIN_VALUE + 1, minimum.incrementTrait(low).ensureTrait(low));
        assertSame(minimum, minimum.incrementTrait(low).decrementTrait(low));
    }

    @Test
    void invalidTraitSetsFailBeforeConstructingStateTables() {
        BooleanBlockTrait first = BooleanBlockTrait.of("test:same");
        BooleanBlockTrait second = BooleanBlockTrait.of("test:same", true);
        assertThrows(IllegalArgumentException.class, () -> BlockRegistry.get()
                .createType(Identifier.parse("test:duplicate"), first, second));

        BlockTrait<?>[] traits = new BlockTrait<?>[31];
        for (int index = 0; index < traits.length; index++) {
            traits[index] = BooleanBlockTrait.of("test:flag_" + index);
        }
        assertThrows(IllegalArgumentException.class, () -> BlockRegistry.get()
                .createType(Identifier.parse("test:too_many_states"), traits));
        assertThrows(IllegalArgumentException.class, () -> BlockTypes.STONE.getDefaultState().ensureTrait(first));
    }

    @Test
    void rangesHandleSingletonsAndRejectOverflowWithoutMaterializingValues() {
        IntRange range = new IntRange(Integer.MAX_VALUE, Integer.MAX_VALUE);
        assertEquals(1, range.size());
        assertEquals(Integer.MAX_VALUE, range.getInt(0));
        assertThrows(UnsupportedOperationException.class, () -> range.add(0));
        assertThrows(IndexOutOfBoundsException.class, () -> range.getInt(1));
        assertThrows(IllegalArgumentException.class, () -> new IntRange(Integer.MIN_VALUE, Integer.MAX_VALUE));
        assertThrows(IllegalArgumentException.class, () -> new IntRange(2, 1));
    }

    private enum Mode {
        CLOSED,
        OPEN,
        EXTRA
    }

    private enum Empty {
    }

    private enum Ambiguous {
        VALUE,
        value
    }
}
