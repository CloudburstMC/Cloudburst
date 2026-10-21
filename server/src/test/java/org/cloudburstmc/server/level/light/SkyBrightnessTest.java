package org.cloudburstmc.server.level.light;

import org.cloudburstmc.api.level.DayCycle;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkyBrightnessTest {

    @Test
    void daylightAndNightUseTheVanillaSkyLightLevels() {
        assertEquals(0, SkyLightEngine.skyDarkness(DayCycle.NOON, 0, 0));
        assertEquals(0, SkyLightEngine.skyDarkness(133, 0, 0));
        assertEquals(0, SkyLightEngine.skyDarkness(11867, 0, 0));
        assertEquals(11, SkyLightEngine.skyDarkness(13670, 0, 0));
        assertEquals(11, SkyLightEngine.skyDarkness(DayCycle.MIDNIGHT, 0, 0));
        assertEquals(11, SkyLightEngine.skyDarkness(22330, 0, 0));
    }

    @Test
    void weatherDarkensDaylightWithoutMakingNightDarker() {
        assertEquals(3, SkyLightEngine.skyDarkness(DayCycle.NOON, 1, 0));
        assertEquals(5, SkyLightEngine.skyDarkness(DayCycle.NOON, 1, 1));
        assertEquals(0, SkyLightEngine.skyDarkness(DayCycle.NOON, 0, 1));
        assertEquals(11, SkyLightEngine.skyDarkness(DayCycle.MIDNIGHT, 1, 1));
    }

    @Test
    void transitionsInterpolateAndRepeatAcrossTheDayBoundary() {
        assertEquals(5, SkyLightEngine.skyDarkness(12768, 0, 0));
        assertEquals(5, SkyLightEngine.skyDarkness(23231, 0, 0));
        for (long time : new long[]{0, 133, 6000, 12768, 18000, 23231, 23999}) {
            int darkness = SkyLightEngine.skyDarkness(time, 0, 0);
            assertEquals(darkness, SkyLightEngine.skyDarkness(time + DayCycle.TICKS_PER_DAY, 0, 0));
            assertEquals(darkness, SkyLightEngine.skyDarkness(time - DayCycle.TICKS_PER_DAY, 0, 0));
            assertTrue(darkness >= 0 && darkness <= 11);
        }
    }
}
