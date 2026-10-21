package org.cloudburstmc.server.block;

import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockTraits;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.api.util.data.DripstoneThickness;
import org.cloudburstmc.server.registry.CloudBlockRegistry;
import org.cloudburstmc.server.testutil.BlockTestRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BlockStateParserTest {

    private static final CloudBlockRegistry REGISTRY = BlockTestRegistry.get();

    @Test
    void omittedPropertiesUseTheBlocksDeclaredDefault() {
        Identifier identifier = BlockTypes.OAK_LOG.getId();
        BlockState expected = BlockTypes.OAK_LOG.getDefaultState();

        assertSame(expected, REGISTRY.getBlock(identifier));
        assertSame(expected, BlockStateParser.parse(identifier.toString()));
    }

    @Test
    void wildcardPropertiesEnumerateOnlyStatesOfTheRequestedBlock() {
        List<BlockState> matches = BlockStateParser.parseMatching("minecraft:cauldron{fill_level=*}").toList();

        assertEquals(7, matches.size());
        assertTrue(matches.stream().allMatch(state -> state.getType() == BlockTypes.CAULDRON));
        assertThrows(IllegalArgumentException.class,
                () -> BlockStateParser.parseMatching("minecraft:cauldron{missing_property=*}"));
    }

    @Test
    void propertiesUseRegisteredNamesAndTypedValues() {
        BlockState expected = BlockTypes.POINTED_DRIPSTONE.getDefaultState()
                .withTrait(BlockTraits.IS_HANGING, true)
                .withTrait(BlockTraits.DRIPSTONE_THICKNESS, DripstoneThickness.BASE);

        assertSame(expected, BlockStateParser.parse(
                " pointed_dripstone { hanging = true, dripstone_thickness = base } "));
        assertSame(expected, BlockStateParser.parse(
                "pointed_dripstone{hanging=1,dripstone_thickness=base}"));
        assertSame(expected, BlockStateParser.parse(
                "pointed_dripstone{\"hanging\"=\"true\",dripstone_thickness=base}"));

        BlockState facing = BlockTypes.SMALL_DRIPLEAF_BLOCK.getDefaultState();
        String direction = BlockPalette.INSTANCE.getDefaultProperties(facing.getType().getId())
                .getString("minecraft:cardinal_direction");

        assertFalse(direction.isEmpty());
        assertSame(facing, BlockStateParser.parse(
                "small_dripleaf_block{minecraft:cardinal_direction=" + direction + '}'));
    }

    @Test
    void wildcardMatchingPreservesOtherPropertyConstraints() {
        List<BlockState> matches = BlockStateParser.parseMatching(
                "pointed_dripstone{hanging=*,dripstone_thickness=base}").toList();

        assertEquals(2, matches.size());
        assertTrue(matches.stream().allMatch(state ->
                state.ensureTrait(BlockTraits.DRIPSTONE_THICKNESS) == DripstoneThickness.BASE));
        assertEquals(2, matches.stream().map(state -> state.ensureTrait(BlockTraits.IS_HANGING)).distinct().count());
    }

    @Test
    void rejectsMalformedExpressionsAndInvalidProperties() {
        List<String> invalid = List.of(
                "", "stone trailing", "stone{} trailing", "cloudburst:missing_block",
                "cauldron{", "cauldron{fill_level}", "cauldron{fill_level=}",
                "cauldron{fill_level=1,}", "cauldron{fill_level=1,,fill_level=2}",
                "cauldron{fill_level=1,fill_level=2}", "cauldron{fill_level=99}",
                "cauldron{unknown=1}", "cauldron{fill_level=*}", "cauldron{\"fill_level=1}");

        for (String input : invalid) {
            assertThrows(IllegalArgumentException.class, () -> BlockStateParser.parse(input), input);
        }

        assertThrows(IllegalArgumentException.class,
                () -> BlockStateParser.parseMatching("cauldron{fill_level=*,fill_level=1}"));
    }
}
