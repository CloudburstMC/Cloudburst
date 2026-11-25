package org.cloudburstmc.server.level.chunk;

import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.server.testutil.BlockTestRegistry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CloudChunkSectionTest {

    @Test
    void randomTickReadsTheCurrentStateAfterOxidation() {
        CloudChunkSection section = new CloudChunkSection(BlockTestRegistry.get());
        BlockState weathered = BlockTypes.WEATHERED_COPPER_CHEST.getDefaultState();
        BlockState oxidized = BlockTypes.OXIDIZED_COPPER_CHEST.getDefaultState();
        section.setBlockState(0, 0, 0, weathered);
        section.setBlockState(1, 0, 0, weathered);
        assertSame(weathered, section.getRandomTickingState(0, 0, 0));

        section.setBlockState(0, 0, 0, oxidized);
        assertNull(section.getRandomTickingState(0, 0, 0));
        assertSame(weathered, section.getRandomTickingState(1, 0, 0));
        assertTrue(section.isRandomlyTicking());

        section.setBlockState(1, 0, 0, oxidized);
        assertNull(section.getRandomTickingState(1, 0, 0));
        assertFalse(section.isRandomlyTicking());
    }
}
