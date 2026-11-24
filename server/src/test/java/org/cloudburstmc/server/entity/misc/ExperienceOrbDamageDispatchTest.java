package org.cloudburstmc.server.entity.misc;

import org.cloudburstmc.api.entity.EntityTypes;
import org.cloudburstmc.api.entity.damage.DamageSource;
import org.cloudburstmc.api.entity.damage.DamageType;
import org.cloudburstmc.api.entity.damage.DamageTypes;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.server.testutil.InterfaceProxy;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ExperienceOrbDamageDispatchTest {

    @Test
    void environmentalAndGenericDamageReachSharedImmunityCheck() {
        for (DamageType type : new DamageType[]{DamageTypes.CACTUS, DamageTypes.LAVA, DamageTypes.GENERIC,
                DamageTypes.ON_FIRE, DamageTypes.OUT_OF_WORLD}) {
            RecordingOrb orb = new RecordingOrb();
            DamageSource source = DamageSource.of(type);

            assertFalse(orb.damage(1, source));
            assertSame(source, orb.checkedSource);
            assertEquals(5, orb.getHealth());
        }
    }

    @Test
    void submergedExplosionsStillReachSharedImmunityCheck() {
        RecordingOrb orb = new RecordingOrb();
        orb.submerged = true;
        DamageSource source = DamageSource.of(DamageTypes.EXPLOSION);

        assertFalse(orb.damage(1, source));
        assertSame(source, orb.checkedSource);
        assertEquals(5, orb.getHealth());
    }

    @Test
    void sharedInvulnerabilityStillRejectsDamage() {
        RecordingOrb orb = new RecordingOrb();
        orb.stopAtImmunity = false;
        orb.setInvulnerable(true);
        DamageSource source = DamageSource.of(DamageTypes.CACTUS);

        assertFalse(orb.damage(1, source));
        assertSame(source, orb.checkedSource);
        assertEquals(5, orb.getHealth());
    }

    private static final class RecordingOrb extends EntityExperienceOrb {
        private DamageSource checkedSource;
        private boolean submerged;
        private boolean stopAtImmunity = true;

        private RecordingOrb() {
            super(EntityTypes.XP_ORB, Location.from(Vector3f.ZERO, InterfaceProxy.create(Level.class)));
            this.setMaxHealth(5);
            this.setHealth(5);
        }

        @Override
        protected boolean isDamageImmune(DamageSource source) {
            this.checkedSource = source;
            // Stop before event dispatch; this fixture has no live event server.
            return this.stopAtImmunity || super.isDamageImmune(source);
        }

        @Override
        public boolean isInsideOfWater() {
            return this.submerged;
        }
    }
}
