package org.cloudburstmc.server.entity.projectile;

import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.EntityTypes;
import org.cloudburstmc.api.entity.projectile.Snowball;
import org.cloudburstmc.api.event.entity.EntityRemoveCause;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.level.particle.ItemParticleOptions;
import org.cloudburstmc.api.util.BlockHitResult;
import org.cloudburstmc.server.entity.CloudEntity;

public class EntitySnowball extends EntityProjectile implements Snowball {

    public EntitySnowball(EntityType<Snowball> type, Location location) {
        super(type, location);
    }

    @Override
    public float getWidth() {
        return 0.25f;
    }

    @Override
    public float getLength() {
        return 0.25f;
    }

    @Override
    public float getHeight() {
        return 0.25f;
    }

    @Override
    public float getGravity() {
        return 0.03f;
    }

    @Override
    public float getDrag() {
        return 0.01f;
    }

    @Override
    protected void onCollideWithEntity(Entity entity) {
        CloudEntity.applyDamage(entity, entity.getType() == EntityTypes.BLAZE ? 3 : 0, this.createProjectileDamageSource());
        this.breakApart();
    }

    @Override
    protected void onBlockCollision(BlockHitResult hit) {
        this.breakApart();
    }

    @Override
    protected boolean tickEntity(int currentTick, int elapsedTicks) {
        if (this.isClosed()) {
            return false;
        }

        boolean hasUpdate = super.tickEntity(currentTick, elapsedTicks);

        if (this.age > 1200) {
            this.remove(EntityRemoveCause.DESPAWN);
            hasUpdate = true;
        }

        return hasUpdate;
    }

    private void breakApart() {
        ItemStack item = ItemStack.from(ItemTypes.SNOWBALL);
        for (int i = 0; i < 8; i++) {
            this.getLevel().spawnParticle(new ItemParticleOptions(item), this.getPosition());
        }

        this.remove(EntityRemoveCause.HIT);
    }
}
