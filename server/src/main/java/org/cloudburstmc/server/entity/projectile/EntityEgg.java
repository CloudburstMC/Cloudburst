package org.cloudburstmc.server.entity.projectile;

import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.EntityTypes;
import org.cloudburstmc.api.entity.projectile.Egg;
import org.cloudburstmc.api.event.entity.CreatureSpawnEvent;
import org.cloudburstmc.api.event.entity.CreatureSpawnReason;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.util.BlockHitResult;
import org.cloudburstmc.server.entity.passive.EntityChicken;
import org.cloudburstmc.server.level.particle.ItemBreakParticle;
import org.cloudburstmc.server.registry.CloudEntityRegistry;

import java.util.concurrent.ThreadLocalRandom;

public class EntityEgg extends EntityProjectile implements Egg {

    public EntityEgg(EntityType<Egg> type, Location location) {
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
        super.onCollideWithEntity(entity);
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

        boolean hasUpdate = super.onUpdate(currentTick);

        if (this.age > 1200) {
            this.kill();
            hasUpdate = true;
        }

        return hasUpdate;
    }

    private void breakApart() {
        ItemStack item = ItemStack.from(ItemTypes.EGG);
        for (int i = 0; i < 8; i++) {
            this.getLevel().addParticle(new ItemBreakParticle(this.getPosition(), item));
        }

        ThreadLocalRandom random = ThreadLocalRandom.current();
        if (random.nextInt(8) == 0) {
            int count = random.nextInt(32) == 0 ? 4 : 1;
            for (int i = 0; i < count; i++) {
                EntityChicken chicken = (EntityChicken) CloudEntityRegistry.get().newEntity(
                        EntityTypes.CHICKEN, Location.from(this.getPosition(), this.getLevel()));
                chicken.setBaby(true);
                chicken.spawn(new CreatureSpawnEvent(chicken, CreatureSpawnReason.EGG));
            }
        }

        this.close();
    }
}
