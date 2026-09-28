package org.cloudburstmc.server.item;

import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockTraits;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.server.item.component.BlockTransformation;
import org.cloudburstmc.server.registry.VanillaBlockTransformations;
import org.junit.jupiter.api.Test;

import static org.cloudburstmc.api.block.BlockTypes.*;
import static org.junit.jupiter.api.Assertions.*;

class BlockTransformationTest {

    @Test
    void tillingRequiresHeadroomAndAnAllowedFace() {
        BlockTransformation rule = VanillaBlockTransformations.HOE.get(GRASS_BLOCK);
        assertNull(rule.apply(GRASS_BLOCK.getDefaultState(), Direction.DOWN, true));
        assertNull(rule.apply(GRASS_BLOCK.getDefaultState(), Direction.UP, false));
        assertSame(FARMLAND.getDefaultState(), rule.apply(GRASS_BLOCK.getDefaultState(), Direction.UP, true));
        assertSame(DIRT.getDefaultState(), VanillaBlockTransformations.HOE.get(COARSE_DIRT).apply(COARSE_DIRT.getDefaultState(), Direction.UP, true));
    }

    @Test
    void rootedDirtTillsWithoutHeadroomAndDropsRoots() {
        BlockTransformation rule = VanillaBlockTransformations.HOE.get(DIRT_WITH_ROOTS);
        assertSame(DIRT.getDefaultState(), rule.apply(DIRT_WITH_ROOTS.getDefaultState(), Direction.DOWN, false));
        assertSame(ItemTypes.HANGING_ROOTS, rule.drop());
    }

    @Test
    void strippingPreservesLogAxis() {
        BlockState log = OAK_LOG.getDefaultState().withTrait(BlockTraits.AXIS, Direction.Axis.X);
        BlockState stripped = VanillaBlockTransformations.AXE.get(OAK_LOG).apply(log, Direction.WEST, false);
        assertNotNull(stripped);
        assertSame(STRIPPED_OAK_LOG, stripped.getType());
        assertSame(Direction.Axis.X, stripped.ensureTrait(BlockTraits.AXIS));
        assertFalse(VanillaBlockTransformations.AXE.containsKey(STRIPPED_OAK_LOG));
        assertSame(FARMLAND.getDefaultState(), FARMLAND.getDefaultState().copyTraits(log));
    }

    @Test
    void copperUnwaxesBeforeScrapingAndPreservesDoorTraits() {
        BlockState waxed = WAXED_OXIDIZED_COPPER_DOOR.getDefaultState().withTrait(BlockTraits.IS_OPEN, true).withTrait(BlockTraits.IS_UPPER_BLOCK, true);
        BlockState unwaxed = VanillaBlockTransformations.AXE.get(waxed.getType()).apply(waxed, Direction.NORTH, false);
        assertNotNull(unwaxed);
        assertSame(OXIDIZED_COPPER_DOOR, unwaxed.getType());
        assertTrue(unwaxed.ensureTrait(BlockTraits.IS_OPEN));
        assertTrue(unwaxed.ensureTrait(BlockTraits.IS_UPPER_BLOCK));
        BlockState scraped = VanillaBlockTransformations.AXE.get(unwaxed.getType()).apply(unwaxed, Direction.NORTH, false);
        assertNotNull(scraped);
        assertSame(WEATHERED_COPPER_DOOR, scraped.getType());
        assertSame(waxed, VanillaBlockTransformations.HONEYCOMB.get(unwaxed.getType()).apply(unwaxed, Direction.NORTH, false));
    }

    @Test
    void everyWaxingRuleHasAnInverse() {
        VanillaBlockTransformations.HONEYCOMB.forEach((type, rule) -> {
            BlockState source = type.getDefaultState();
            BlockState waxed = rule.apply(source, Direction.UP, true);
            assertNotNull(waxed);
            assertTrue(rule.consumesItem());
            assertSame(source, VanillaBlockTransformations.AXE.get(waxed.getType()).apply(waxed, Direction.UP, true));
        });
    }

    @Test
    void shovelsFlattenSoilAndDouseCampfiresWithoutChangingTheirDirection() {
        BlockTransformation flatten = VanillaBlockTransformations.SHOVEL.get(PODZOL);
        assertNull(flatten.apply(PODZOL.getDefaultState(), Direction.DOWN, true));
        assertNull(flatten.apply(PODZOL.getDefaultState(), Direction.UP, false));
        assertSame(GRASS_PATH.getDefaultState(), flatten.apply(PODZOL.getDefaultState(), Direction.UP, true));

        BlockState lit = SOUL_CAMPFIRE.getDefaultState().withTrait(BlockTraits.IS_EXTINGUISHED, false);
        BlockTransformation douse = VanillaBlockTransformations.SHOVEL.get(SOUL_CAMPFIRE);
        BlockState extinguished = douse.apply(lit, Direction.DOWN, false);
        assertNotNull(extinguished);
        assertTrue(extinguished.ensureTrait(BlockTraits.IS_EXTINGUISHED));
        assertSame(lit.ensureTrait(BlockTraits.CARDINAL_DIRECTION), extinguished.ensureTrait(BlockTraits.CARDINAL_DIRECTION));
        assertNull(douse.apply(extinguished, Direction.UP, true));
    }
}
