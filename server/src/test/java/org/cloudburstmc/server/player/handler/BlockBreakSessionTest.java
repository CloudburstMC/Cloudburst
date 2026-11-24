package org.cloudburstmc.server.player.handler;

import org.cloudburstmc.api.block.BlockComponents;
import org.cloudburstmc.api.block.BlockStates;
import org.cloudburstmc.api.block.BlockTraits;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.api.util.data.TurtleEggCount;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.registry.CloudBlockRegistry;
import org.cloudburstmc.server.testutil.BlockTestRegistry;
import org.cloudburstmc.server.testutil.BlockTestWorld;
import org.cloudburstmc.server.testutil.InterfaceProxy;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class BlockBreakSessionTest {

    private static final CloudBlockRegistry REGISTRY = BlockTestRegistry.get();
    private static final Vector3i POSITION = Vector3i.from(87, 76, 276);

    @Test
    void multipleActionsInOneInputTickDoNotAccelerateMining() {
        BlockBreakSession session = new BlockBreakSession(POSITION, BlockStates.STONE, ItemStack.EMPTY, Direction.UP, 0.2D, 10);

        session = session.advance(Direction.UP, 0.2D, 11);
        assertSame(session, session.advance(Direction.UP, 0.2D, 11));
        assertFalse(session.canConfirmPrediction());

        session = session.advance(Direction.UP, 0.2D, 12);
        assertFalse(session.canConfirmPrediction());
        assertTrue(session.advance(Direction.UP, 0.2D, 13).canConfirmPrediction());
    }

    @Test
    void continuousMiningTracksEveryEggWithoutANewStartAction() {
        BlockTestWorld world = new BlockTestWorld(REGISTRY);
        world.setState(POSITION, BlockStates.TURTLE_EGG.withTrait(BlockTraits.TURTLE_EGG_COUNT, TurtleEggCount.FOUR_EGG));
        Player player = InterfaceProxy.create(Player.class, Map.of("isCreative", false));
        long inputTick = 0;
        BlockBreakSession session = new BlockBreakSession(POSITION, world.state(POSITION), ItemStack.EMPTY, Direction.UP, 1d / 15, inputTick);

        for (int remaining = 3; remaining >= 0; remaining--) {
            assertNotNull(session);
            assertEquals(remaining == 3 ? 1d / 15 : 0, session.progress(), 1e-6);

            int ticksUntilPrediction = remaining == 3 ? 14 : 15;
            for (int tick = 0; tick < ticksUntilPrediction; tick++) {
                session = session.advance(Direction.UP, 1d / 15, ++inputTick);
            }

            assertEquals(1, session.progress(), 1e-6);
            assertSame(world.state(POSITION), session.state());

            REGISTRY.requireComponent(BlockTypes.TURTLE_EGG, BlockComponents.ON_DESTROY).execute(world.block(POSITION), player);
            session = session.continueAfterBreak(world.state(POSITION), ItemStack.EMPTY);
            if (remaining > 0) {
                assertNotNull(session);
                assertEquals(remaining, session.state().ensureTrait(BlockTraits.TURTLE_EGG_COUNT).ordinal() + 1);
                assertEquals(POSITION, session.position());
                assertSame(session, session.advance(Direction.UP, 1d / 15, inputTick));
            }
        }

        assertNull(session);
        assertSame(BlockStates.AIR, world.state(POSITION));
    }

    @Test
    void completedBlocksDoNotCarryMiningIntoAirLiquidsOrAnUnchangedState() {
        BlockTestWorld world = new BlockTestWorld(REGISTRY);
        world.setState(POSITION, BlockStates.STONE);
        BlockBreakSession session = new BlockBreakSession(POSITION, world.state(POSITION), ItemStack.EMPTY, Direction.UP, 1, 0);

        assertNull(session.continueAfterBreak(BlockStates.AIR, ItemStack.EMPTY));
        assertNull(session.continueAfterBreak(BlockStates.WATER, ItemStack.EMPTY));
        assertNull(session.continueAfterBreak(BlockStates.STONE, ItemStack.EMPTY));
    }
}
