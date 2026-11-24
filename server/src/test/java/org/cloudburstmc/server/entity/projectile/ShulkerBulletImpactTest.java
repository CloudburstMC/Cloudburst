package org.cloudburstmc.server.entity.projectile;

import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.EntityTypes;
import org.cloudburstmc.api.entity.damage.DamageSource;
import org.cloudburstmc.api.entity.damage.DamageTypes;
import org.cloudburstmc.api.event.entity.PotionEffectCause;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.level.RayTraceContext;
import org.cloudburstmc.api.potion.EffectTypes;
import org.cloudburstmc.api.potion.PotionEffect;
import org.cloudburstmc.api.util.HitResult;
import org.cloudburstmc.api.util.MissHitResult;
import org.cloudburstmc.api.util.MissReason;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.server.entity.CloudLivingEntity;
import org.cloudburstmc.server.testutil.InterfaceProxy;
import org.cloudburstmc.server.testutil.PersistenceFixtures;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ShulkerBulletImpactTest {

    @Test
    void nearbyTargetSteersFlightAndDisablesGravity() {
        EntityShulkerBullet bullet = new FlightTestBullet();
        Entity target = InterfaceProxy.create(Entity.class,
                Map.of("isAlive", true, "getPosition", Vector3f.from(1, 0, 0), "getHeight", 0f));
        bullet.setTarget(target);

        bullet.tickEntity(1, 1);

        assertSame(target, bullet.getTarget());
        assertTrue(bullet.getMotion().getX() > 0);
        assertEquals(0f, bullet.getMotion().getY());
        assertEquals(0f, bullet.getGravity());

        bullet.setTarget(null);
        assertNull(bullet.getTarget());
        assertEquals(0.04f, bullet.getGravity());
    }

    @Test
    void acceptedHitDealsFixedDamageAndAppliesLevitation() {
        EntityShulkerBullet bullet = createBullet();
        HitTarget target = new HitTarget(true);

        bullet.onCollideWithEntity(target);

        assertEquals(4f, target.damage);
        assertSame(DamageTypes.MOB_PROJECTILE, target.damageSource.getDamageType());
        assertSame(bullet, target.damageSource.getDirectEntity());
        assertSame(EffectTypes.LEVITATION, target.effect.getType());
        assertEquals(200, target.effect.getDuration());
        assertSame(bullet, target.effectSource);
        assertEquals(PotionEffectCause.ATTACK, target.effectCause);
        assertTrue(bullet.isClosed());
    }

    @Test
    void rejectedDamageDoesNotApplyLevitation() {
        EntityShulkerBullet bullet = createBullet();
        HitTarget target = new HitTarget(false);

        bullet.onCollideWithEntity(target);

        assertEquals(4f, target.damage);
        assertNull(target.effect);
        assertTrue(bullet.isClosed());
    }

    private static EntityShulkerBullet createBullet() {
        return assertInstanceOf(EntityShulkerBullet.class, PersistenceFixtures.entity(EntityTypes.SHULKER_BULLET));
    }

    private static class FlightTestBullet extends EntityShulkerBullet {

        private FlightTestBullet() {
            super(EntityTypes.SHULKER_BULLET, Location.from(Vector3f.ZERO, InterfaceProxy.create(Level.class)));
            this.recalculateBoundingBox();
        }

        @Override
        public boolean entityBaseTick(int elapsedTicks) {
            return false;
        }

        @Override
        public boolean isInsideOfWater() {
            return false;
        }

        @Override
        protected HitResult traceMovement(RayTraceContext trace, float hitMargin) {
            return new MissHitResult(this.getPosition(), MissReason.UNLOADED);
        }

        @Override
        public void updateMovement() {
        }
    }

    private static class HitTarget extends CloudLivingEntity {

        private final boolean acceptDamage;
        private float damage;
        private DamageSource damageSource;
        private PotionEffect effect;
        private Entity effectSource;
        private PotionEffectCause effectCause;

        private HitTarget(boolean acceptDamage) {
            super(EntityTypes.ZOMBIE, Location.from(Vector3f.ZERO, InterfaceProxy.create(Level.class)));
            this.acceptDamage = acceptDamage;
        }

        @Override
        public boolean damage(float amount, DamageSource source) {
            this.damage = amount;
            this.damageSource = source;
            return this.acceptDamage;
        }

        @Override
        public boolean addPotionEffect(PotionEffect effect, Entity source, PotionEffectCause cause) {
            this.effect = effect;
            this.effectSource = source;
            this.effectCause = cause;
            return true;
        }
    }
}
