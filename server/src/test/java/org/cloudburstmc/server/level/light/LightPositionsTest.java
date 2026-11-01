package org.cloudburstmc.server.level.light;

import org.cloudburstmc.api.util.Direction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LightPositionsTest {

    @Test
    void packingRoundTripsNegativeAndExtremeCoordinates() {
        int[] horizontal = {-30_000_000, -1, 0, 15, 16, 29_999_999};
        int[] vertical = {-64, -1, 0, 319};
        for (int x : horizontal) {
            for (int z : horizontal) {
                for (int y : vertical) {
                    long packed = LightPositions.pack(x, y, z);
                    assertEquals(x, LightPositions.x(packed), () -> "x of " + x + "," + y + "," + z);
                    assertEquals(y, LightPositions.y(packed), () -> "y of " + x + "," + y + "," + z);
                    assertEquals(z, LightPositions.z(packed), () -> "z of " + x + "," + y + "," + z);
                }
            }
        }

        long down = LightPositions.offset(LightPositions.pack(0, -64, 0), Direction.DOWN);
        assertEquals(0, LightPositions.x(down));
        assertEquals(-65, LightPositions.y(down));
        assertEquals(0, LightPositions.z(down));

        long east = LightPositions.offset(LightPositions.pack(-1, 5, -1), Direction.EAST);
        assertEquals(0, LightPositions.x(east));
        assertEquals(5, LightPositions.y(east));
        assertEquals(-1, LightPositions.z(east));
    }
}
