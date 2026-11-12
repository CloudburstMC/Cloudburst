package org.cloudburstmc.server.block.component;

import org.cloudburstmc.api.block.BlockStates;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.block.NoteBlockInstrument;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.testutil.BlockTestRegistry;
import org.cloudburstmc.server.testutil.BlockTestWorld;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NoteBlockHandlersTest {

    @Test
    void selectsTheInstrumentFromSupportUnlessAHeadOverridesIt() {
        BlockTestWorld world = new BlockTestWorld(BlockTestRegistry.get());
        Vector3i position = Vector3i.from(0, 64, 0);
        Vector3i below = Direction.DOWN.relative(position);
        Vector3i above = Direction.UP.relative(position);
        world.setState(position, BlockStates.NOTE_BLOCK);

        world.setState(below, BlockStates.OAK_PLANKS);
        assertEquals(NoteBlockInstrument.BASS, NoteBlockHandlers.resolveInstrument(world.block(position)));

        world.setState(below, BlockTypes.WAXED_EXPOSED_CUT_COPPER.getDefaultState());
        assertEquals(NoteBlockInstrument.TRUMPET_EXPOSED, NoteBlockHandlers.resolveInstrument(world.block(position)));

        world.setState(above, BlockTypes.ZOMBIE_HEAD.getDefaultState());
        assertEquals(NoteBlockInstrument.ZOMBIE, NoteBlockHandlers.resolveInstrument(world.block(position)));

        world.setState(above, BlockStates.AIR);
        world.setState(below, BlockTypes.ZOMBIE_HEAD.getDefaultState());
        assertEquals(NoteBlockInstrument.HARP, NoteBlockHandlers.resolveInstrument(world.block(position)));
    }
}
