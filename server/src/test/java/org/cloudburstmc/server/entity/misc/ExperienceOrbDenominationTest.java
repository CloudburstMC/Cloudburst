package org.cloudburstmc.server.entity.misc;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExperienceOrbDenominationTest {

    @Test
    void splitsExperienceIntoDescendingDenominations() {
        List<Integer> values = new ArrayList<>();
        EntityExperienceOrb.forEachOrbValue(5000, values::add);

        assertEquals(List.of(2477, 2477, 37, 7, 1, 1), values);
    }

    @Test
    void nonPositiveExperienceProducesNoOrbs() {
        List<Integer> values = new ArrayList<>();
        EntityExperienceOrb.forEachOrbValue(0, values::add);
        EntityExperienceOrb.forEachOrbValue(-1, values::add);

        assertTrue(values.isEmpty());
    }
}
