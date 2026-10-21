package org.cloudburstmc.server.testutil;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.BlockComponents;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.data.ComponentType;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.registry.CloudBlockRegistry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BlockTestWorldTest {

    @Test
    void componentAccessUsesTheCapturedBlockTypeAndRefreshUsesTheReplacement() {
        CloudBlockRegistry registry = BlockTestRegistry.get();
        BlockTestWorld world = new BlockTestWorld(registry);
        Vector3i position = Vector3i.from(1, 64, 2);
        world.setState(position, BlockTypes.SEA_LANTERN.getDefaultState());
        Block block = world.block(position);

        assertSame(registry.requireComponents(BlockTypes.SEA_LANTERN), block.getComponents());
        assertSame(block.getComponents().require(BlockComponents.GET_LOOT), block.requireComponent(BlockComponents.GET_LOOT));
        assertSame(block.requireComponent(BlockComponents.GET_LOOT), block.getComponent(BlockComponents.GET_LOOT));
        assertEquals(position.getX(), block.getX());

        ComponentType<String> missing = ComponentType.of("test_missing", String.class);
        assertNull(block.getComponent(missing));
        assertThrows(IllegalStateException.class, () -> block.requireComponent(missing));

        world.setState(position, BlockTypes.STONE.getDefaultState());
        assertSame(registry.requireComponents(BlockTypes.SEA_LANTERN), block.getComponents());
        assertSame(registry.requireComponents(BlockTypes.STONE), block.refresh().getComponents());
    }
}
