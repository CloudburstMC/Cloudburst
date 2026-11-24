package org.cloudburstmc.server.player;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PlayerExperienceTest {

    @Test
    void givingPointsCarriesAcrossLevels() {
        PlayerExperience experience = new PlayerExperience();

        experience.givePoints(17);

        assertEquals(2, experience.level());
        assertEquals(1, experience.points());
    }

    @Test
    void changingLevelKeepsTheFractionalProgress() {
        PlayerExperience experience = new PlayerExperience();
        experience.setProgress(28, 18);

        experience.setLevel(14);

        assertEquals(14, experience.level());
        assertEquals(18, experience.points());
    }

    @Test
    void savedValuesAreNormalizedWithoutChangingTheSeed() {
        PlayerExperience experience = new PlayerExperience();
        experience.load(-10, -1, 123);

        assertEquals(0, experience.points());
        assertEquals(0, experience.level());
        assertEquals(123, experience.enchantmentSeed());
    }

    @Test
    void givingPointsAtTheLargestLevelCannotOverflow() {
        PlayerExperience experience = new PlayerExperience();
        experience.setProgress(Integer.MAX_VALUE - 2, Integer.MAX_VALUE);

        experience.givePoints(Integer.MAX_VALUE);

        assertEquals(Integer.MAX_VALUE, experience.level());
        assertEquals(Integer.MAX_VALUE - 1, experience.points());
    }

    @Test
    void givingNegativePointsDoesNotChangeProgress() {
        PlayerExperience experience = new PlayerExperience();
        experience.givePoints(3);

        assertThrows(IllegalArgumentException.class, () -> experience.givePoints(-1));
        assertEquals(3, experience.points());
    }
}
