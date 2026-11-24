package org.cloudburstmc.server.entity.projectile;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.EntityTypes;
import org.cloudburstmc.api.event.entity.ProjectileHitEvent;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.level.RayTraceContext;
import org.cloudburstmc.api.potion.PotionType;
import org.cloudburstmc.api.util.BlockHitResult;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.api.util.HitResult;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.server.testutil.InterfaceProxy;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ThrownPotionImpactLifecycleTest {

    @Test
    void closedPotionsIgnoreLateEntityImpacts() {
        Entity target = InterfaceProxy.create(Entity.class);
        for (EntityAbstractThrownPotion potion : potions()) {
            potion.close();

            assertDoesNotThrow(() -> potion.onCollideWithEntity(target));
            assertDoesNotThrow(() -> potion.onCollideWithEntity(target));
            assertTrue(potion.isClosed());
        }
    }

    @Test
    void closedPotionsIgnoreLateBlockSplashes() {
        for (EntityAbstractThrownPotion potion : potions()) {
            potion.close();

            assertDoesNotThrow(() -> potion.splash(null));
            assertDoesNotThrow(() -> potion.splash(null));
            assertTrue(potion.isClosed());
        }
    }

    @Test
    void removalDuringBaseTickDoesNotSplashStaleCollisionContact() {
        BlockImpactPotion potion = new BlockImpactPotion();

        assertTrue(potion.tickEntity(1, 1));
        assertEquals(1, potion.splashes);
        assertFalse(potion.isClosed());

        potion.closeOnBaseTick = true;
        potion.tickEntity(2, 1);

        assertTrue(potion.isClosed());
        assertEquals(1, potion.splashes);
    }

    private static List<EntityAbstractThrownPotion> potions() {
        Location location = Location.from(Vector3f.ZERO, InterfaceProxy.create(Level.class));
        return List.of(new EntitySplashPotion(EntityTypes.SPLASH_POTION, location) {
            @Override
            public PotionType getPotionType() {
                return fail("Closed splash potion must not read its payload");
            }

            @Override
            protected void applyPotionImpact(Entity collidedWith) {
                fail("Closed splash potion must not apply effects");
            }
        }, new EntityLingeringPotion(EntityTypes.LINGERING_POTION, location) {
            @Override
            public PotionType getPotionType() {
                return fail("Closed lingering potion must not read its payload");
            }

            @Override
            protected void applyPotionImpact(Entity collidedWith) {
                fail("Closed lingering potion must not create a cloud");
            }
        });
    }

    private static class BlockImpactPotion extends EntitySplashPotion {

        private boolean closeOnBaseTick;
        private int splashes;

        private BlockImpactPotion() {
            super(EntityTypes.SPLASH_POTION, Location.from(Vector3f.ZERO, InterfaceProxy.create(Level.class)));
            this.recalculateBoundingBox();
        }

        @Override
        public boolean entityBaseTick(int elapsedTicks) {
            if (this.closeOnBaseTick) {
                this.close();
            }
            return false;
        }

        @Override
        public boolean isInsideOfWater() {
            return false;
        }

        @Override
        protected HitResult traceMovement(RayTraceContext trace, float hitMargin) {
            return new BlockHitResult(Vector3f.from(1, 1, 0), InterfaceProxy.create(Block.class), Direction.UP, false);
        }

        @Override
        protected void dispatchHitEvent(ProjectileHitEvent event) {
            event.setCancelled();
        }

        @Override
        public void updateMovement() {
        }

        @Override
        protected void splash(Entity collidedWith) {
            this.splashes++;
        }
    }
}
