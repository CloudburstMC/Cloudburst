package org.cloudburstmc.server.item;

import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockTraits;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.server.item.component.BlockTransformation;
import org.cloudburstmc.server.registry.VanillaBlockTransformations;
import org.junit.jupiter.api.Test;

import static org.cloudburstmc.api.block.BlockTypes.*;
import static org.cloudburstmc.api.util.Direction.*;
import static org.cloudburstmc.server.registry.VanillaBlockTransformations.*;
import static org.junit.jupiter.api.Assertions.*;

class BlockTransformationTest {

    @Test
    void tillingRequiresHeadroomAndAnAllowedFace() {
        BlockState grass = GRASS_BLOCK.getDefaultState();
        assertNull(HOE.resolve(grass, DOWN, true));
        assertNull(HOE.resolve(grass, UP, false));

        BlockTransformation till = HOE.resolve(grass, UP, true);
        assertNotNull(till);
        assertSame(FARMLAND.getDefaultState(), till.state());
    }

    @Test
    void rootedDirtTillsWithoutHeadroomAndDropsRoots() {
        BlockTransformation till = HOE.resolve(DIRT_WITH_ROOTS.getDefaultState(), DOWN, false);
        assertNotNull(till);
        assertSame(DIRT.getDefaultState(), till.state());
        assertSame(ItemTypes.HANGING_ROOTS, till.drop());
    }

    @Test
    void strippingPreservesLogAxis() {
        BlockState log = OAK_LOG.getDefaultState().withTrait(BlockTraits.AXIS, Direction.Axis.X);
        BlockTransformation strip = AXE.resolve(log, WEST, false);
        assertNotNull(strip);
        assertSame(STRIPPED_OAK_LOG, strip.state().getType());
        assertSame(Direction.Axis.X, strip.state().ensureTrait(BlockTraits.AXIS));
        assertNull(AXE.resolve(strip.state(), WEST, false));
    }

    @Test
    void copperUnwaxesBeforeScrapingAndPreservesDoorTraits() {
        BlockState waxed = WAXED_OXIDIZED_COPPER_DOOR.getDefaultState()
                .withTrait(BlockTraits.IS_OPEN, true)
                .withTrait(BlockTraits.IS_UPPER_BLOCK, true);
        BlockTransformation unwax = AXE.resolve(waxed, NORTH, false);
        assertNotNull(unwax);

        BlockState unwaxed = unwax.state();
        assertSame(OXIDIZED_COPPER_DOOR, unwaxed.getType());
        assertTrue(unwaxed.ensureTrait(BlockTraits.IS_OPEN));
        assertTrue(unwaxed.ensureTrait(BlockTraits.IS_UPPER_BLOCK));

        BlockTransformation scrape = AXE.resolve(unwaxed, NORTH, false);
        assertNotNull(scrape);
        assertSame(WEATHERED_COPPER_DOOR, scrape.state().getType());

        BlockTransformation wax = VanillaBlockTransformations.HONEYCOMB.resolve(unwaxed, NORTH, false);
        assertNotNull(wax);
        assertSame(waxed, wax.state());
        assertTrue(wax.consumesItem());
    }

    @Test
    void shovelsFlattenSoilAndDouseCampfiresWithoutChangingTheirDirection() {
        BlockState podzol = PODZOL.getDefaultState();
        assertNull(SHOVEL.resolve(podzol, DOWN, true));
        assertNull(SHOVEL.resolve(podzol, UP, false));

        BlockTransformation flatten = SHOVEL.resolve(podzol, UP, true);
        assertNotNull(flatten);
        assertSame(GRASS_PATH.getDefaultState(), flatten.state());

        BlockState lit = SOUL_CAMPFIRE.getDefaultState().withTrait(BlockTraits.IS_EXTINGUISHED, false);
        BlockTransformation douse = SHOVEL.resolve(lit, DOWN, false);
        assertNotNull(douse);
        assertTrue(douse.state().ensureTrait(BlockTraits.IS_EXTINGUISHED));
        assertSame(lit.ensureTrait(BlockTraits.CARDINAL_DIRECTION), douse.state().ensureTrait(BlockTraits.CARDINAL_DIRECTION));
        assertNull(SHOVEL.resolve(douse.state(), UP, true));
    }
}
