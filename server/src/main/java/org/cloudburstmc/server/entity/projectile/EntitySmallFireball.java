package org.cloudburstmc.server.entity.projectile;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.Mob;
import org.cloudburstmc.api.entity.projectile.SmallFireball;
import org.cloudburstmc.api.event.block.BlockIgniteCause;
import org.cloudburstmc.api.event.block.BlockIgniteEvent;
import org.cloudburstmc.api.event.entity.EntityCombustByEntityEvent;
import org.cloudburstmc.api.event.entity.EntityRemoveCause;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.level.gamerule.GameRules;
import org.cloudburstmc.api.util.BlockHitResult;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.block.component.FireBlockHandlers;
import org.cloudburstmc.server.entity.CloudEntity;
import org.cloudburstmc.server.level.CloudLevel;

public class EntitySmallFireball extends EntityProjectile implements SmallFireball {

    private static final float IMPACT_DAMAGE = 5f;
    private static final int BURN_SECONDS = 5;

    public EntitySmallFireball(EntityType<SmallFireball> entityType, Location location) {
        super(entityType, location);
    }

    @Override
    protected void onCollideWithEntity(Entity target) {
        if (CloudEntity.applyDamage(target, IMPACT_DAMAGE, this.createProjectileDamageSource()) && target.isAlive() && !this.isClosed()) {
            EntityCombustByEntityEvent event = new EntityCombustByEntityEvent(this, target, BURN_SECONDS);
            this.getServer().getEventManager().fire(event);
            if (!event.isCancelled() && target.isAlive()) {
                target.setOnFire(event.getDuration());
            }
        }
    }

    @Override
    protected void onBlockCollision(BlockHitResult hit) {
        Direction face = hit.face();
        CloudLevel level = this.getLevel();
        if (face == null || (this.getOwner() instanceof Mob && !level.getGameRules().get(GameRules.MOB_GRIEFING))) {
            return;
        }

        Vector3i position = face.relative(hit.block().getPosition());
        Block target = level.getLoadedBlock(position);
        if (target == null || target.getState().getType() != BlockTypes.AIR) {
            return;
        }

        BlockIgniteEvent event = new BlockIgniteEvent(target, BlockIgniteCause.FIREBALL, this, null);
        this.getServer().getEventManager().fire(event);
        if (!event.isCancelled() && !this.isClosed()) {
            Block current = level.getLoadedBlock(position);
            if (current != null && current.getState().getType() == BlockTypes.AIR) {
                level.setBlockState(position, FireBlockHandlers.placementState(current), false, true);
            }
        }
    }

    @Override
    protected void afterHit() {
        this.remove(EntityRemoveCause.HIT);
    }
}
