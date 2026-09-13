package org.cloudburstmc.server.item.component;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.entity.Creature;
import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.event.entity.CreatureSpawnEvent;
import org.cloudburstmc.api.event.entity.CreatureSpawnReason;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.component.UseOnHandler;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.server.entity.CloudEntity;
import org.cloudburstmc.server.registry.CloudEntityRegistry;

@UtilityClass
public class SpawnEggItemHandlers {

    public static UseOnHandler useOn(EntityType<?> entityType) {
        return (item, entity, blockPos, face, clickPos) -> {
            float spawnX = blockPos.getX() + 0.5f;
            float spawnY = blockPos.getY() + 1.0f;
            float spawnZ = blockPos.getZ() + 0.5f;

            Location location = Location.from(spawnX, spawnY, spawnZ, entity.getLevel());
            CloudEntity spawned = (CloudEntity) CloudEntityRegistry.get().newEntity(entityType, location);
            boolean spawnedSuccessfully = spawned instanceof Creature creature
                    ? spawned.spawn(new CreatureSpawnEvent(creature, CreatureSpawnReason.SPAWN_EGG))
                    : spawned.spawn();
            if (!spawnedSuccessfully) {
                return item;
            }

            if (((Player) entity).isCreative()) {
                return item;
            }

            ItemStack result = item.decreaseCount();
            return result.getCount() <= 0 ? ItemStack.EMPTY : result;
        };
    }
}
