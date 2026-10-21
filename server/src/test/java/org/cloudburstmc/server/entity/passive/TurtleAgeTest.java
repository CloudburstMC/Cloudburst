package org.cloudburstmc.server.entity.passive;

import org.cloudburstmc.api.entity.EntityTypes;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.server.testutil.InterfaceProxy;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TurtleAgeTest {

    @Test
    void changingAgeUpdatesBabyMetadataAndCollisionDimensions() {
        Level level = InterfaceProxy.create(Level.class, Map.of());
        EntityTurtle turtle = new EntityTurtle(EntityTypes.TURTLE, Location.from(level));

        turtle.setAge(-24000);

        assertEquals(-24000, turtle.getAge());
        assertTrue(turtle.isBaby());
        assertEquals(0.36f, turtle.getWidth());
        assertEquals(0.12f, turtle.getHeight());
        assertEquals(0.36f, turtle.getBoundingBox().getMaxX() - turtle.getBoundingBox().getMinX(), 0.00001f);

        turtle.setAge(0);

        assertFalse(turtle.isBaby());
        assertEquals(1.2f, turtle.getWidth());
        assertEquals(0.4f, turtle.getHeight());
        assertEquals(1.2f, turtle.getBoundingBox().getMaxX() - turtle.getBoundingBox().getMinX(), 0.00001f);
    }
}
