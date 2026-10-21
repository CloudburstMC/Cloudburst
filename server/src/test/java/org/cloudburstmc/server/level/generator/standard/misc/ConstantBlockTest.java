package org.cloudburstmc.server.level.generator.standard.misc;

import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.server.registry.CloudBlockRegistry;
import org.cloudburstmc.server.testutil.BlockTestRegistry;
import org.junit.jupiter.api.Test;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.random.RandomGenerator;

import static org.junit.jupiter.api.Assertions.*;

class ConstantBlockTest {

    private static final CloudBlockRegistry REGISTRY = BlockTestRegistry.get();

    @Test
    void selectsAndMatchesOnlyItsState() {
        BlockState state = REGISTRY.getBlock(BlockTypes.STONE);
        ConstantBlock block = new ConstantBlock(state);

        assertSame(state, block.state());
        assertTrue(block.test(state));
        assertFalse(block.test(REGISTRY.getBlock(BlockTypes.DIRT)));
        assertEquals(1, block.size());
        assertEquals(1, block.sizeWeighted());
        assertEquals(1, block.weight());
        assertSame(state, block.get(0));
        assertSame(state, block.getWeighted(0));
        assertSame(state, block.selectWeighted(RandomGenerator.getDefault()));
        assertEquals(List.of(state), block.states().toList());
        assertEquals(List.of(block), block.entries().toList());
        assertThrows(IndexOutOfBoundsException.class, () -> block.get(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> block.getWeighted(1));
        assertThrows(NullPointerException.class, () -> new ConstantBlock(null));
    }

    @Test
    void configurationRequiresANamedStateString() {
        JsonMapper json = JsonMapper.builder().build();
        BlockState state = REGISTRY.getBlock(BlockTypes.GRANITE);

        ConstantBlock block = json.readValue("\"minecraft:granite\"", ConstantBlock.class);

        assertSame(state, block.state());
        assertEquals(new ConstantBlock(state), ConstantBlock.parse("minecraft:granite"));
        assertThrows(JacksonException.class, () -> json.readValue("{\"id\":\"minecraft:stone\",\"meta\":1}", ConstantBlock.class));
        assertThrows(JacksonException.class, () -> json.readValue("{\"state\":\"minecraft:stone\"}", ConstantBlock.class));
        assertThrows(IllegalArgumentException.class, () -> ConstantBlock.parse("1:0"));
        assertThrows(IllegalArgumentException.class, () -> ConstantBlock.parse("cloudburst:missing_block"));
    }
}
