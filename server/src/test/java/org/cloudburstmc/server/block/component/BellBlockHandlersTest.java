package org.cloudburstmc.server.block.component;

import org.cloudburstmc.api.block.BlockStates;
import org.cloudburstmc.api.block.BlockTraits;
import org.cloudburstmc.api.blockentity.Bell;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.api.util.data.AttachmentType;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.testutil.BlockTestRegistry;
import org.cloudburstmc.server.testutil.BlockTestWorld;
import org.cloudburstmc.server.testutil.InterfaceProxy;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BellBlockHandlersTest {

    private static final Vector3i POSITION = Vector3i.from(0, 64, 0);

    @Test
    void doubleWallBellReattachesToTheRemainingWall() {
        BlockTestWorld world = new BlockTestWorld(BlockTestRegistry.get());
        Vector3i removedSupport = Direction.WEST.relative(POSITION);
        world.setState(Direction.EAST.relative(POSITION), BlockStates.STONE);
        world.setState(POSITION, BlockStates.BELL.withTrait(BlockTraits.ATTACHMENT, AttachmentType.MULTIPLE)
                .withTrait(BlockTraits.DIRECTION, Direction.EAST));

        BellBlockHandlers.ON_NEIGHBOUR_CHANGED.execute(world.block(POSITION), world.block(removedSupport));

        assertEquals(AttachmentType.SIDE, world.state(POSITION).ensureTrait(BlockTraits.ATTACHMENT));
        assertEquals(Direction.WEST, world.state(POSITION).ensureTrait(BlockTraits.DIRECTION));
    }

    @Test
    void powerRingsOncePerRisingEdgeEvenWhenTheRingIsRejected() {
        BlockTestWorld world = new BlockTestWorld(BlockTestRegistry.get());
        Vector3i source = Direction.EAST.relative(POSITION);
        AtomicInteger attempts = new AtomicInteger();
        Bell bell = InterfaceProxy.create(Bell.class, (proxy, method, arguments) -> {
            if (!method.getName().equals("ring")) {
                throw new AssertionError(method);
            }
            attempts.incrementAndGet();
            return false;
        });
        world.setBlockEntity(POSITION, bell);
        world.setState(POSITION, BlockStates.BELL.withTrait(BlockTraits.IS_TOGGLED, false));
        world.setState(source, BlockStates.REDSTONE_BLOCK);

        BellBlockHandlers.UPDATE_POWER.execute(world.block(POSITION));
        BellBlockHandlers.UPDATE_POWER.execute(world.block(POSITION));
        assertEquals(1, attempts.get());

        world.setState(source, BlockStates.AIR);
        BellBlockHandlers.UPDATE_POWER.execute(world.block(POSITION));
        world.setState(source, BlockStates.REDSTONE_BLOCK);
        BellBlockHandlers.UPDATE_POWER.execute(world.block(POSITION));
        assertEquals(2, attempts.get());
    }
}
