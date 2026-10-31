package org.cloudburstmc.server.testutil;

import org.cloudburstmc.api.blockentity.Bed;
import org.cloudburstmc.api.blockentity.BlockEntityTypes;
import org.cloudburstmc.math.vector.Vector3i;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Proves {@link PersistenceFixtures} works in isolation, without any other
 * persistence test class having run first.
 */
class PersistenceFixturesTest {

    @Test
    void fixtureBuildsRealBlockEntityInIsolation() {
        Bed bed = PersistenceFixtures.blockEntity(BlockEntityTypes.BED, Vector3i.from(0, 64, 0));

        assertNotNull(bed);
    }
}
