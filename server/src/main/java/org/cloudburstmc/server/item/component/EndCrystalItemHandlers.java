package org.cloudburstmc.server.item.component;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.block.BlockType;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.entity.EntityTypes;
import org.cloudburstmc.api.event.entity.EntityPlaceEvent;
import org.cloudburstmc.api.event.entity.EntityRemoveCause;
import org.cloudburstmc.api.item.ItemDataComponents;
import org.cloudburstmc.api.item.component.UseOnHandler;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.util.BoundingBox;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.entity.misc.EntityEnderCrystal;
import org.cloudburstmc.server.level.CloudLevel;
import org.cloudburstmc.server.registry.CloudEntityRegistry;

@UtilityClass
public class EndCrystalItemHandlers {

    public static final UseOnHandler USE_ON = (item, entity, position, face, click) -> {
        CloudLevel level = (CloudLevel) entity.getLevel();
        BlockType support = level.getBlockState(position).getType();
        if (support != BlockTypes.OBSIDIAN && support != BlockTypes.BEDROCK) {
            return item;
        }

        Vector3i above = position.add(0, 1, 0);
        if (level.getBlockState(above).getType() != BlockTypes.AIR
                || !level.getNearbyEntities(new BoundingBox(
                above.getX(), above.getY(), above.getZ(),
                above.getX() + 1, above.getY() + 2, above.getZ() + 1)).isEmpty()) {
            return item;
        }

        Vector3f spawnPosition = above.toFloat().add(0.5f, 0, 0.5f);
        EntityEnderCrystal crystal = (EntityEnderCrystal) CloudEntityRegistry.get().createFromItem(item, EntityTypes.ENDER_CRYSTAL, Location.from(spawnPosition, level));
        if (!item.has(ItemDataComponents.ENTITY_DATA)) {
            crystal.setShowingBase(false);
        }

        Player player = entity instanceof Player placer ? placer : null;
        EntityPlaceEvent event = new EntityPlaceEvent(crystal, player, level.getBlock(position), face);
        level.getServer().getEventManager().fire(event);
        if (event.isCancelled() || crystal.getLevel() != level || !crystal.spawn()) {
            crystal.remove(EntityRemoveCause.DISCARD);
            return item;
        }

        if (level.getDimension() == CloudLevel.DIMENSION_THE_END) {
            level.getEndFight().initiateRespawn();
        }

        return player != null && player.isCreative() ? item : item.decreaseCount();
    };
}
