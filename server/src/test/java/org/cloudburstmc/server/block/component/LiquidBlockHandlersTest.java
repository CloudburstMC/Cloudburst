package org.cloudburstmc.server.block.component;

import org.cloudburstmc.api.block.*;
import org.cloudburstmc.api.util.Identifier;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LiquidBlockHandlersTest {

    @Test
    void identifiesLiquidsThatCanOccupyTheSecondaryLayer() {
        assertTrue(LiquidBlockHandlers.canOccupySecondaryLayer(liquidState("water", LiquidTypes.WATER)));
        assertTrue(LiquidBlockHandlers.canOccupySecondaryLayer(liquidState("flowing_water", LiquidTypes.FLOWING_WATER)));
        assertFalse(LiquidBlockHandlers.canOccupySecondaryLayer(liquidState("lava", LiquidTypes.LAVA)));
        assertFalse(LiquidBlockHandlers.canOccupySecondaryLayer(liquidState("flowing_lava", LiquidTypes.FLOWING_LAVA)));
    }

    private static LiquidState liquidState(String id, LiquidType liquidType) {
        BlockType blockType = BlockType.of(Identifier.parse("test:" + id), BlockTraits.LIQUID_DEPTH);
        BlockRegistrationAccess.bindLiquidType(blockType, liquidType);
        return LiquidState.of(blockType.getDefaultState());
    }
}
