package org.cloudburstmc.server.block.component;

import org.cloudburstmc.api.block.BlockStates;
import org.cloudburstmc.api.block.LiquidState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LiquidBlockHandlersTest {

    @Test
    void identifiesLiquidsThatCanOccupyTheSecondaryLayer() {
        assertTrue(LiquidBlockHandlers.canOccupySecondaryLayer(LiquidState.of(BlockStates.WATER)));
        assertTrue(LiquidBlockHandlers.canOccupySecondaryLayer(LiquidState.of(BlockStates.FLOWING_WATER)));
        assertFalse(LiquidBlockHandlers.canOccupySecondaryLayer(LiquidState.of(BlockStates.LAVA)));
        assertFalse(LiquidBlockHandlers.canOccupySecondaryLayer(LiquidState.of(BlockStates.FLOWING_LAVA)));
    }
}
