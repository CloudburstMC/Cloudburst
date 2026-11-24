package org.cloudburstmc.server.entity.projectile;

import org.cloudburstmc.api.entity.EntityTypes;
import org.cloudburstmc.api.entity.projectile.EyeOfEnderSignal;
import org.cloudburstmc.api.entity.projectile.Projectile;
import org.cloudburstmc.server.testutil.PersistenceFixtures;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class EyeOfEnderSignalHierarchyTest {

    @Test
    void signalDoesNotExposeTheCombatProjectileContract() {
        EyeOfEnderSignal signal = PersistenceFixtures.entity(EntityTypes.EYE_OF_ENDER_SIGNAL);

        assertInstanceOf(EntityEyeOfEnderSignal.class, signal);
        assertFalse(Projectile.class.isAssignableFrom(EyeOfEnderSignal.class));
        assertFalse(signal instanceof Projectile);
    }
}
