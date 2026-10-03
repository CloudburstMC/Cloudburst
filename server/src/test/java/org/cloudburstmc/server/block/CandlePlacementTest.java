package org.cloudburstmc.server.block;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockTraits;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.server.block.component.CandleBlockHandlers;
import org.cloudburstmc.server.testutil.InterfaceProxy;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CandlePlacementTest {

    @Test
    void stacksMatchingCandlesUpToFourWithoutResettingLitState() {
        Player player = InterfaceProxy.create(Player.class);
        BlockState state = BlockTypes.RED_CANDLE.getDefaultState().withTrait(BlockTraits.IS_LIT, true);

        for (int count = 1; count <= 4; count++) {
            Block block = InterfaceProxy.create(Block.class, Map.of("getState", state));
            boolean replaceable = CandleBlockHandlers.CAN_BE_REPLACED.execute(block, BlockTypes.RED_CANDLE.getDefaultState(), player, Direction.UP, Vector3f.ZERO);
            assertEquals(count < 4, replaceable);
            if (replaceable) {
                state = CandleBlockHandlers.RESOLVE_PLACEMENT_STATE.execute(BlockTypes.RED_CANDLE.getDefaultState(), block, player, Direction.UP, Vector3f.ZERO);
                assertEquals(count, state.ensureTrait(BlockTraits.CANDLES));
                assertTrue(state.ensureTrait(BlockTraits.IS_LIT));
            }
        }
    }

    @Test
    void rejectsDifferentColoursForStacking() {
        Block block = InterfaceProxy.create(Block.class, Map.of("getState", BlockTypes.CANDLE.getDefaultState()));
        assertFalse(CandleBlockHandlers.CAN_BE_REPLACED.execute(block, BlockTypes.RED_CANDLE.getDefaultState(), null, Direction.UP, Vector3f.ZERO));
    }

    @Test
    void combinesCandlesWhileSneakPlacingAgainstAnAdjacentBlock() {
        Player sneaking = InterfaceProxy.create(Player.class, Map.of("isSneaking", true));
        BlockState state = BlockTypes.CANDLE.getDefaultState().withTrait(BlockTraits.IS_LIT, true);
        for (int count = 1; count <= 4; count++) {
            Block block = InterfaceProxy.create(Block.class, Map.of("getState", state));
            boolean replaceable = CandleBlockHandlers.CAN_BE_REPLACED.execute(
                    block, BlockTypes.CANDLE.getDefaultState(), sneaking, Direction.EAST, Vector3f.from(1, 0.5f, 0.5f));
            assertEquals(count < 4, replaceable);
            if (replaceable) {
                state = CandleBlockHandlers.RESOLVE_PLACEMENT_STATE.execute(BlockTypes.CANDLE.getDefaultState(), block, sneaking, Direction.EAST, Vector3f.from(1, 0.5f, 0.5f));
                assertEquals(count, state.ensureTrait(BlockTraits.CANDLES));
                assertTrue(state.ensureTrait(BlockTraits.IS_LIT));
            }
        }
    }

    @Test
    void rejectsStandaloneCandlePlacementAboveCake() {
        Block block = InterfaceProxy.create(Block.class, Map.of("getRelativeState", BlockTypes.CAKE.getDefaultState()));
        assertFalse(CandleBlockHandlers.CAN_SURVIVE.execute(block));
    }
}
