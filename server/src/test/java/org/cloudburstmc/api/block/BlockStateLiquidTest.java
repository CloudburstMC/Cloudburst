package org.cloudburstmc.api.block;

import org.cloudburstmc.api.util.Identifier;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BlockStateLiquidTest {

    @Test
    void decodesBedrockLiquidDepth() {
        assertLiquidState(8, true, false, 0);
        assertLiquidState(7, false, false, 1);
        assertLiquidState(1, false, false, 7);
        assertLiquidState(8, false, true, 8);
        assertLiquidState(8, false, true, 15);
    }

    @Test
    void identifiesLiquidBlockTypes() {
        assertTrue(BlockTypes.WATER.isLiquid());
        assertTrue(BlockTypes.FLOWING_WATER.isLiquid());
        assertTrue(BlockTypes.LAVA.isLiquid());
        assertTrue(BlockTypes.FLOWING_LAVA.isLiquid());
        assertFalse(BlockTypes.AIR.isLiquid());
    }

    @Test
    void separatesLiquidTypesFromBlockTypes() {
        LiquidState water = liquidState(0);

        assertSame(LiquidTypes.FLOWING_WATER, water.getType());
        assertTrue(water.getType().isSameFamily(LiquidTypes.WATER));
        assertFalse(water.getType().isSameFamily(LiquidTypes.LAVA));
        assertSame(LiquidTypes.EMPTY, LiquidState.empty().getType());
    }

    private static void assertLiquidState(int amount, boolean source, boolean falling, int liquidDepth) {
        LiquidState liquid = liquidState(liquidDepth);
        assertEquals(amount, liquid.getAmount());
        assertEquals(source, liquid.isSource());
        assertEquals(falling, liquid.isFalling());
        assertEquals(amount / 9f, liquid.getOwnHeight());
    }

    private static LiquidState liquidState(int liquidDepth) {
        BlockType type = BlockType.of(Identifier.parse("test:flowing_water"), BlockTraits.LIQUID_DEPTH);
        BlockRegistrationAccess.bindLiquidType(type, LiquidTypes.FLOWING_WATER);
        BlockState state = type.getDefaultState().withTrait(BlockTraits.LIQUID_DEPTH, liquidDepth);
        LiquidState liquid = LiquidState.of(state);
        assertSame(liquid, LiquidState.of(state));
        return liquid;
    }
}
