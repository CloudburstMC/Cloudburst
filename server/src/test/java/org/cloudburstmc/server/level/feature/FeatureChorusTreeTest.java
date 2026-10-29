package org.cloudburstmc.server.level.feature;

import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockTraits;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.block.component.ChorusBlockHandlers;
import org.cloudburstmc.server.level.generator.GenerationRegion;
import org.cloudburstmc.server.level.generator.standard.misc.IntRange;
import org.cloudburstmc.server.testutil.BlockTestRegistry;
import org.cloudburstmc.server.testutil.BlockTestWorld;
import org.cloudburstmc.server.testutil.InterfaceProxy;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationHandler;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FeatureChorusTreeTest {

    @Test
    void generatedPlantsConnectToTheirFinalNeighborsWithoutLiveBlockCallbacks() {
        BlockTestWorld world = new BlockTestWorld(BlockTestRegistry.get());
        world.setState(Vector3i.from(0, 63, 0), BlockTypes.END_STONE.getDefaultState());
        GenerationRegion region = InterfaceProxy.create(GenerationRegion.class, (proxy, method, arguments) -> {
            if (method.isDefault()) {
                return InvocationHandler.invokeDefault(proxy, method, arguments);
            }
            Vector3i position = Vector3i.from((int) arguments[0], (int) arguments[1], (int) arguments[2]);
            return switch (method.getName()) {
                case "getBlockState" -> world.state(position);
                case "setBlockState" -> {
                    world.setState(position, (BlockState) arguments[4]);
                    yield true;
                }
                default -> throw new AssertionError("Unexpected generation access: " + method);
            };
        });

        assertTrue(new FeatureChorusTree(new IntRange(2), 1, 8).place(region, new Random(0), 0, 64, 0));

        BlockState root = world.state(Vector3i.from(0, 64, 0));
        assertTrue(root.ensureTrait(BlockTraits.CONNECTION_DOWN));
        assertTrue(root.ensureTrait(BlockTraits.CONNECTION_UP));
        world.getStates().forEach((position, state) -> {
            if (state.getType() == BlockTypes.CHORUS_PLANT) {
                assertSame(ChorusBlockHandlers.resolveConnections(state,
                        direction -> world.state(direction.relative(position))), state, position.toString());
            }
        });
    }
}
