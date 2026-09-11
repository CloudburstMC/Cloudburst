package org.cloudburstmc.server.item.component;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.component.UseOnHandler;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.server.block.util.RailConnector;
import org.cloudburstmc.server.entity.CloudEntity;
import org.cloudburstmc.server.level.CloudLevel;
import org.cloudburstmc.server.registry.CloudEntityRegistry;

@UtilityClass
public class MinecartItemHandlers {

    public static UseOnHandler useOn(EntityType<?> entityType) {
        return (item, entity, blockPos, face, clickPos) -> {
            CloudLevel level = (CloudLevel) entity.getLevel();
            BlockState railState = level.getBlockState(blockPos.getX(), blockPos.getY(), blockPos.getZ());

            if (!RailConnector.isRail(railState)) {
                return item;
            }

            float yOffset = RailConnector.getDirection(railState).isAscending() ? 0.5f : 0.0f;

            float spawnX = blockPos.getX() + 0.5f;
            float spawnY = blockPos.getY() + 0.5f + yOffset;
            float spawnZ = blockPos.getZ() + 0.5f;

            Location location = Location.from(spawnX, spawnY, spawnZ, level);
            CloudEntity spawned = (CloudEntity) CloudEntityRegistry.get().newEntity(entityType, location);
            spawned.spawnToAll();

            if (((Player) entity).isCreative()) {
                return item;
            }

            ItemStack result = item.decreaseCount();
            return result.getCount() <= 0 ? ItemStack.EMPTY : result;
        };
    }
}
