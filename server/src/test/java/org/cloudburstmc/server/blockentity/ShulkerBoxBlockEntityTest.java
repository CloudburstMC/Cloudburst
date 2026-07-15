package org.cloudburstmc.server.blockentity;

import org.cloudburstmc.api.util.BoundingBox;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.math.vector.Vector3i;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ShulkerBoxBlockEntityTest {

    @Test
    void openingSweepExtendsAlongFacingDirection() {
        Vector3i position = Vector3i.from(2, 3, 4);

        assertEquals(new BoundingBox(2, 4, 4, 3, 4.5f, 5),
                ShulkerBoxBlockEntity.lidMovementBox(position, Direction.UP, 0, 0.5f));
        assertEquals(new BoundingBox(1.5f, 3, 4, 2, 4, 5),
                ShulkerBoxBlockEntity.lidMovementBox(position, Direction.WEST, 0, 0.5f));
    }
}
