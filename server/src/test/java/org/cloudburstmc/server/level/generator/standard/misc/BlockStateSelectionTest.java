package org.cloudburstmc.server.level.generator.standard.misc;

import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockTraits;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.util.data.DripstoneThickness;
import org.cloudburstmc.server.level.generator.standard.misc.filter.AnyOfBlockFilter;
import org.cloudburstmc.server.level.generator.standard.misc.selector.BlockSelector;
import org.cloudburstmc.server.registry.CloudBlockRegistry;
import org.cloudburstmc.server.testutil.BlockTestRegistry;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.*;

class BlockStateSelectionTest {

    private static final CloudBlockRegistry REGISTRY = BlockTestRegistry.get();
    private static final String EXPRESSION = "pointed_dripstone{hanging=*,dripstone_thickness=base}";

    @Test
    void filtersMatchStatesInsteadOfNetworkDefinitions() {
        AnyOfBlockFilter filter = new AnyOfBlockFilter(new String[]{EXPRESSION, "stone"});
        BlockState base = BlockTypes.POINTED_DRIPSTONE.getDefaultState()
                .withTrait(BlockTraits.DRIPSTONE_THICKNESS, DripstoneThickness.BASE);

        assertTrue(filter.test(base.withTrait(BlockTraits.IS_HANGING, true)));
        assertTrue(filter.test(base.withTrait(BlockTraits.IS_HANGING, false)));
        assertTrue(filter.test(REGISTRY.getBlock(BlockTypes.STONE)));
        assertFalse(filter.test(REGISTRY.getBlock(BlockTypes.DIRT)));
        assertFalse(filter.test(base.withTrait(BlockTraits.DRIPSTONE_THICKNESS, DripstoneThickness.TIP)));
    }

    @Test
    void selectorEntriesKeepPropertyCommasAndWeightsIntact() {
        JsonMapper json = JsonMapper.builder().build();
        String input = "[\"2*" + EXPRESSION + "\",\"stone\"]";

        BlockSelector selector = json.readValue(input, BlockSelector.class);

        assertEquals(3, selector.size());
        assertEquals(5, selector.sizeWeighted());
        assertEquals(2, selector.entries().filter(entry -> entry.weight() == 2).count());
        assertTrue(selector.states().anyMatch(state -> state == REGISTRY.getBlock(BlockTypes.STONE)));
    }

    @Test
    void aSingleSelectorEntryRetainsItsWeight() {
        JsonMapper json = JsonMapper.builder().build();

        BlockSelector selector = json.readValue("[\"3*stone\"]", BlockSelector.class);

        assertEquals(1, selector.size());
        assertEquals(3, selector.sizeWeighted());
        assertEquals(3, selector.entries().findFirst().orElseThrow().weight());
    }

    @Test
    void eachFilterArrayEntryMustContainOneExpression() {
        assertThrows(IllegalArgumentException.class, () -> new AnyOfBlockFilter(new String[]{"stone,dirt"}));
    }
}
