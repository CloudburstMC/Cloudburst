package org.cloudburstmc.server.item.component;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.entity.EntityTypes;
import org.cloudburstmc.api.event.entity.EntityPlaceEvent;
import org.cloudburstmc.api.item.ItemDataComponents;
import org.cloudburstmc.api.item.component.UseOnHandler;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.protocol.bedrock.data.LevelEvent;
import org.cloudburstmc.server.entity.CloudEntitySnapshot;
import org.cloudburstmc.server.entity.misc.EntityPainting;
import org.cloudburstmc.server.level.CloudLevel;
import org.cloudburstmc.server.player.CloudPlayer;

@UtilityClass
public class PaintingItemHandlers {

    public static final UseOnHandler USE_ON = (item, entity, position, face, click) -> {
        if (!(entity instanceof CloudPlayer player) || player.isAdventure() || player.isSpectator() || face == Direction.UP || face == Direction.DOWN) {
            return item;
        }

        CloudLevel level = player.getLevel();
        Vector3i anchor = face.relative(position);
        if (!player.canInteractWithBlock(anchor)) {
            return item;
        }

        Block clicked = level.getBlock(position);
        float yaw = face.getHorizontalIndex() * 90f;
        EntityPainting painting = (EntityPainting) CloudEntitySnapshot.createFromItem(item, EntityTypes.PAINTING, Location.from(anchor.toFloat(), yaw, 0, level));
        painting.setRotation(yaw, 0);

        boolean fits = item.get(ItemDataComponents.ENTITY_DATA) == null ? painting.selectLargestVariant() : painting.fitsWall();
        if (!fits) {
            painting.close();
            return item;
        }

        EntityPlaceEvent event = new EntityPlaceEvent(painting, player, clicked, face);
        player.getServer().getEventManager().fire(event);
        if (event.isCancelled() || painting.getLevel() != level || !painting.spawnOnWall()) {
            painting.close();
            return item;
        }

        painting.spawnToAll();
        level.addLevelEvent(clicked.getPosition().toFloat(), LevelEvent.SOUND_ITEMFRAME_ITEM_ADD, 0);
        return player.isCreative() ? item : item.decreaseCount();
    };
}
