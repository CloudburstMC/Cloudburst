package org.cloudburstmc.server.entity.system;

import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.EntityComponentTypes;
import org.cloudburstmc.api.entity.component.Consumable;
import org.cloudburstmc.api.entity.system.EntitySystem;
import org.cloudburstmc.api.entity.system.EntitySystemRunner;
import org.cloudburstmc.api.util.AxisAlignedBB;

public class ConsumableEntitySystem implements EntitySystemRunner {
    public static final EntitySystem SYSTEM = EntitySystem.builder()
            .expectComponent(EntityComponentTypes.CONSUMABLE)
            .runner(new ConsumableEntitySystem())
            .build();

    @Override
    public void run(Entity entity) {
        Consumable consumable = entity.ensureAndGet(EntityComponentTypes.CONSUMABLE);
        if (consumable.hasInfiniteDelay()) {
            return;
        }

        int delay = consumable.getDelay();
        consumable.setDelay(--delay);

        if (consumable.canConsume()) {
            AxisAlignedBB boundingBox = consumable.getBoundingBox();
            if (boundingBox == null) {
                boundingBox = entity.getBoundingBox();
            }

            for (Entity colliding : entity.getLevel().getCollidingEntities(boundingBox)) {
                if (consumable.consume(colliding)) {
                    return;
                }
            }
        }
    }
}
