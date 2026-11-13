package org.cloudburstmc.server.block.util;

import org.cloudburstmc.api.block.BlockStates;
import org.cloudburstmc.api.entity.EntityTypes;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.entity.misc.EntityFallingBlock;
import org.cloudburstmc.server.testutil.InterfaceProxy;
import org.cloudburstmc.server.testutil.TestLevels;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BlockShapeTransitionsTest {

    @Test
    void liftsAnOverlappingFallingBlockToTheSurfaceOfTheNewBlock() {
        TestFallingBlock entity = new TestFallingBlock(Vector3f.from(13.5f, 76.25f, 67.5f));

        addSolidBlock(entity);

        assertEquals(Vector3f.from(13.5f, 77, 67.5f), entity.getPosition());
    }

    @Test
    void leavesAnEntityTouchingTheSurfaceInPlace() {
        Vector3f position = Vector3f.from(13.5f, 77, 67.5f);
        TestFallingBlock entity = new TestFallingBlock(position);

        addSolidBlock(entity);

        assertEquals(position, entity.getPosition());
    }

    private static void addSolidBlock(TestFallingBlock entity) {
        Level level = TestLevels.level(-64, 320, Map.of("getNearbyEntities", Set.of(entity)));
        BlockShapeTransitions.pushEntitiesUp(level, Vector3i.from(13, 76, 67), BlockStates.AIR, BlockStates.STONE);
    }

    private static class TestFallingBlock extends EntityFallingBlock {

        private TestFallingBlock(Vector3f position) {
            super(EntityTypes.FALLING_BLOCK, Location.from(position, InterfaceProxy.create(Level.class)));
            this.setPosition(position);
        }

        @Override
        public void sendAuthoritativeDisplacement() {
        }
    }
}
