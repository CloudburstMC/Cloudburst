package org.cloudburstmc.server.entity.projectile;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.entity.EntityTypes;
import org.cloudburstmc.api.event.entity.ProjectileHitEvent;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.level.RayTraceContext;
import org.cloudburstmc.api.util.BlockHitResult;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.api.util.HitResult;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.server.testutil.InterfaceProxy;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProjectileBlockImpactTest {

    @Test
    void cancelledBlockReactionDoesNotSuppressProjectileImpact() {
        Block block = InterfaceProxy.create(Block.class,
                (_, _, _) -> fail("Cancelled impact must not notify the block"));
        BlockHitResult hit = new BlockHitResult(Vector3f.from(1, 1, 0), block, Direction.UP, false);
        ImpactProjectile projectile = new ImpactProjectile(hit);

        assertTrue(projectile.tickEntity(1, 1));

        assertEquals(hit.position(), projectile.getPosition());
        assertEquals(Vector3f.ZERO, projectile.getMotion());
        assertEquals(1, projectile.blockCollisions);
        assertEquals(1, projectile.afterHits);
    }

    private static class ImpactProjectile extends EntityProjectile {

        private final BlockHitResult hit;
        private int blockCollisions;
        private int afterHits;

        private ImpactProjectile(BlockHitResult hit) {
            super(EntityTypes.SNOWBALL, Location.from(Vector3f.ZERO, InterfaceProxy.create(Level.class)));
            this.hit = hit;
            this.recalculateBoundingBox();
            this.motion = Vector3f.from(0, -1, 0);
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
            return this.hit;
        }

        @Override
        protected void dispatchHitEvent(ProjectileHitEvent event) {
            assertSame(this.hit, event.getHitResult());
            event.setCancelled();
        }

        @Override
        protected void onBlockCollision(BlockHitResult hit) {
            this.blockCollisions++;
        }

        @Override
        protected void afterHit() {
            this.afterHits++;
        }

        @Override
        public void updateMovement() {
        }
    }
}
