package org.cloudburstmc.server.item.component;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.component.BooleanItemHandler;
import org.cloudburstmc.api.item.component.UseOnHandler;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.server.entity.CloudEntity;
import org.cloudburstmc.server.registry.EntityRegistry;

@UtilityClass
public class SpawnEggItemHandlers {

    public static final BooleanItemHandler CAN_BE_USED = (item) -> true;

    public static UseOnHandler useOn(EntityType<?> entityType) {
        return (item, entity, blockPos, face, clickPos) -> {
            float spawnX = blockPos.getX() + 0.5f;
            float spawnY = blockPos.getY() + 1.0f;
            float spawnZ = blockPos.getZ() + 0.5f;

            Location location = Location.from(spawnX, spawnY, spawnZ, entity.getLevel());
            CloudEntity spawned = (CloudEntity) EntityRegistry.get().newEntity(entityType, location);
            spawned.spawnToAll();

            if (((Player) entity).isCreative()) {
                return item;
            }

            ItemStack result = item.decreaseCount();
            return result.getCount() <= 0 ? ItemStack.EMPTY : result;
        };
    }
}
