package org.cloudburstmc.server.block.component;

import org.cloudburstmc.api.block.component.BlockShapeContext;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.testutil.InterfaceProxy;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BlockShapeContextTest {

    @Test
    void emptyContextRejectsLevelAndPositionAccess() {
        BlockShapeContext context = BlockShapeContext.empty();

        assertTrue(context.isEmpty());
        assertThrows(IllegalStateException.class, context::level);
        assertThrows(IllegalStateException.class, context::position);
    }

    @Test
    void positionedContextRetainsItsLevelAndPositionWithoutQueryingThem() {
        Level level = InterfaceProxy.create(Level.class);
        Vector3i position = Vector3i.from(12, 64, -8);

        BlockShapeContext context = BlockShapeContext.at(level, position);

        assertFalse(context.isEmpty());
        assertSame(level, context.level());
        assertSame(position, context.position());
    }
}
