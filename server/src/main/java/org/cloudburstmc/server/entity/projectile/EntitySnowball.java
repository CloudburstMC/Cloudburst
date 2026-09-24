package org.cloudburstmc.server.entity.projectile;

import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.EntityTypes;
import org.cloudburstmc.api.entity.projectile.Snowball;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.util.BlockHitResult;
import org.cloudburstmc.server.level.particle.ItemBreakParticle;

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
        entity.damage(entity.getType() == EntityTypes.BLAZE ? 3 : 0, this.createProjectileDamageSource());
        this.breakApart();
    }

    @Override
    protected void onBlockCollision(BlockHitResult hit) {
        this.breakApart();
    }

    @Override
    public boolean onUpdate(int currentTick) {
        if (this.closed) {
            return false;
        }

        this.timing.startTiming();

        boolean hasUpdate = super.onUpdate(currentTick);

        if (this.age > 1200) {
            this.kill();
            hasUpdate = true;
        }

        this.timing.stopTiming();

        return hasUpdate;
    }

    private void breakApart() {
        ItemStack item = ItemStack.from(ItemTypes.SNOWBALL);
        for (int i = 0; i < 8; i++) {
            this.getLevel().addParticle(new ItemBreakParticle(this.getPosition(), item));
        }

        this.close();
    }
}
