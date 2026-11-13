package org.cloudburstmc.server.item.component;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.entity.EntityTypes;
import org.cloudburstmc.api.event.entity.EntityPlaceEvent;
import org.cloudburstmc.api.item.ItemDataComponents;
import org.cloudburstmc.api.item.component.UseOnHandler;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.entity.CloudEntitySnapshot;
import org.cloudburstmc.server.entity.misc.EntityArmorStand;
import org.cloudburstmc.server.level.CloudLevel;
import org.cloudburstmc.server.player.CloudPlayer;

@UtilityClass
public class ArmorStandItemHandlers {

    public static final UseOnHandler USE_ON = (item, entity, position, face, click) -> {
        if (!(entity instanceof CloudPlayer player) || player.isSpectator() || face == Direction.DOWN) {
            return item;
        }

        CloudLevel level = player.getLevel();
        Block clicked = level.getBlock(position);
        Vector3i target = clicked.getState().isReplaceable() ? position : face.relative(position);
        if (!player.canInteractWithBlock(target)) {
            return item;
        }

        Vector3f feet = target.toFloat().add(0.5f, 0, 0.5f);
        float yaw = Math.round((player.getYaw() - 180) / 45f) * 45f;
        EntityArmorStand stand = (EntityArmorStand) CloudEntitySnapshot.createFromItem(item, EntityTypes.ARMOR_STAND, Location.from(feet, yaw, 0, level));
        if (stand.isClosed() || !stand.isAlive() || isObstructed(level, stand)) {
            stand.close();
            return item;
        }

        String name = item.get(ItemDataComponents.CUSTOM_NAME);
        if (name != null) {
            stand.setNameTag(name);
        }

        EntityPlaceEvent event = new EntityPlaceEvent(stand, player, clicked, face);
        player.getServer().getEventManager().fire(event);
        if (event.isCancelled() || stand.isClosed() || stand.getLevel() != level || isObstructed(level, stand) || !stand.spawn()) {
            stand.close();
            return item;
        }

        stand.spawnToAll();
        stand.playPlacementSound();
        return player.isCreative() ? item : item.decreaseCount();
    };

    private static boolean isObstructed(CloudLevel level, EntityArmorStand stand) {
        return stand.getBoundingBox().getMinY() < level.getMinHeight()
                || stand.getBoundingBox().getMaxY() > level.getMaxHeight()
                || level.hasCollision(stand)
                || !level.getNearbyEntities(stand, stand.getBoundingBox()).isEmpty();
    }
}
