package org.cloudburstmc.server.level.light;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LightSeedBandsTest {

    @Test
    void seedBandsStayWithinTheirBandSize() {
        for (int bands : new int[]{1, 8, 16}) {
            int width = (16 + bands - 1) / bands;
            int covered = 0;

            for (int band = 0; band < bands; band++) {
                int current = band;
                int fromX = LightPositions.bandFromX(current, bands);
                int toX = LightPositions.bandToX(current, bands);
                assertTrue(toX - fromX <= width, () -> "band " + current + " of " + bands + " is wider than " + width);
                assertEquals(covered, fromX, () -> "band " + current + " of " + bands + " leaves a gap");
                covered = toX;
            }

            assertEquals(16, covered, () -> bands + " bands do not cover the chunk");
        }

        for (int total : new int[]{0, 1, LightRegion.UPDATE_BAND_POSITIONS - 1, LightRegion.UPDATE_BAND_POSITIONS, LightRegion.UPDATE_BAND_POSITIONS + 1, 300}) {
            int bands = LightRegion.updateBands(total);
            assertEquals(Math.max(1, (total + LightRegion.UPDATE_BAND_POSITIONS - 1) / LightRegion.UPDATE_BAND_POSITIONS), bands);
            int covered = 0;

            for (int band = 0; band < bands; band++) {
                int current = band;
                int from = LightRegion.updateBandFrom(total, current);
                int to = LightRegion.updateBandTo(total, current);
                assertTrue(to - from <= LightRegion.UPDATE_BAND_POSITIONS, () -> "update band " + current + " of " + total + " exceeds its size");
                assertEquals(covered, from, () -> "update band " + current + " of " + total + " leaves a gap");
                covered = to;
            }

            assertEquals(total, covered, () -> "update bands do not cover " + total + " positions");
        }

        int unloadBands = LightRegion.unloadBands(-64, 320);
        assertEquals((384 + LightRegion.UNLOAD_BAND_HEIGHTS - 1) / LightRegion.UNLOAD_BAND_HEIGHTS, unloadBands);
        int coveredY = -64;

        for (int band = 0; band < unloadBands; band++) {
            int current = band;
            int fromY = LightRegion.unloadBandFromY(-64, current);
            int toY = LightRegion.unloadBandToY(-64, 320, current);
            assertTrue(toY - fromY <= LightRegion.UNLOAD_BAND_HEIGHTS, () -> "unload band " + current + " exceeds its size");
            assertEquals(coveredY, fromY, () -> "unload band " + current + " leaves a gap");
            coveredY = toY;
        }

        assertEquals(320, coveredY, "unload bands do not cover the height range");
    }
}
