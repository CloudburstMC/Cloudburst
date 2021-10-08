package org.cloudburstmc.server.entity.system;

import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.EntityComponentTypes;
import org.cloudburstmc.api.entity.component.Explodable;
import org.cloudburstmc.api.entity.system.EntitySystem;
import org.cloudburstmc.api.entity.system.EntitySystemRunner;
import org.cloudburstmc.api.event.entity.EntityExplosionPrimeEvent;
import org.cloudburstmc.api.level.gamerule.GameRules;
import org.cloudburstmc.server.level.CloudLevel;
import org.cloudburstmc.server.level.Explosion;

public class ExplodableEntitySystem implements EntitySystemRunner {
    public static final EntitySystem SYSTEM = EntitySystem.builder()
            .runner(new ExplodableEntitySystem())
            .expectComponent(EntityComponentTypes.EXPLODABLE)
            .build();

    @Override
    public void run(Entity entity) {
        Explodable explodable = entity.ensureAndGet(EntityComponentTypes.EXPLODABLE);

        if (!explodable.isPrimed()) {
            return;
        }

        int fuse = explodable.getFuse();
        explodable.setFuse(--fuse);

        if (fuse > 0) {
            return;
        }

        // Fuse has ended so we can start the explosion.
        if (entity.getLevel().getGameRules().get(GameRules.TNT_EXPLODES)) {
            EntityExplosionPrimeEvent event = new EntityExplosionPrimeEvent(entity, 4);
            entity.getServer().getEventManager().fire(event);
            if (event.isCancelled()) {
                return;
            }
            Explosion explosion = new Explosion((CloudLevel) entity.getLevel(), entity.getPosition(), event.getForce(), entity);
            if (event.isBlockBreaking()) {
                explosion.explodeA();
            }
            explosion.explodeB();
        }
        entity.kill();
    }
}
