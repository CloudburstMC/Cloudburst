package org.cloudburstmc.server.block.component;

import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockStates;
import org.cloudburstmc.api.block.BlockTraits;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.testutil.BlockTestRegistry;
import org.cloudburstmc.server.testutil.BlockTestWorld;
import org.cloudburstmc.server.testutil.InterfaceProxy;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class EndRodBlockHandlersTest {

    private static final Vector3i POSITION = Vector3i.from(0, 64, 0);

    @Test
    void placesAlongTheClickedFaceRegardlessOfPlayerLookDirection() {
        BlockTestWorld world = new BlockTestWorld(BlockTestRegistry.get());
        Player player = InterfaceProxy.create(Player.class,
                Map.of("getPitch", 90f, "getHorizontalDirection", Direction.WEST));

        for (Direction face : Direction.values()) {
            world.setState(face.getOpposite().relative(POSITION), BlockStates.STONE);

            BlockState placed = EndRodBlockHandlers.RESOLVE_PLACEMENT_STATE.execute(
                    BlockStates.END_ROD, world.block(POSITION), player, face, Vector3f.ZERO
            );

            assertNotNull(placed);
            assertSame(face, placed.ensureTrait(BlockTraits.FACING_DIRECTION));
        }
    }

    @Test
    void reversesOnlyWhenPlacedAgainstAnotherRodsTip() {
        BlockTestWorld world = new BlockTestWorld(BlockTestRegistry.get());

        for (Direction face : Direction.values()) {
            Direction opposite = face.getOpposite();
            Vector3i behind = opposite.relative(POSITION);
            world.setState(behind, BlockStates.END_ROD.withTrait(BlockTraits.FACING_DIRECTION, face));

            BlockState tip = EndRodBlockHandlers.RESOLVE_PLACEMENT_STATE.execute(
                    BlockStates.END_ROD, world.block(POSITION), null, face, Vector3f.ZERO
            );
            assertNotNull(tip);
            assertSame(opposite, tip.ensureTrait(BlockTraits.FACING_DIRECTION));

            world.setState(behind, BlockStates.END_ROD.withTrait(BlockTraits.FACING_DIRECTION, opposite));

            BlockState base = EndRodBlockHandlers.RESOLVE_PLACEMENT_STATE.execute(
                    BlockStates.END_ROD, world.block(POSITION), null, face, Vector3f.ZERO
            );
            assertNotNull(base);
            assertSame(face, base.ensureTrait(BlockTraits.FACING_DIRECTION));
        }
    }
}
