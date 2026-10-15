package org.cloudburstmc.server.item.component;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.item.ItemDataComponents;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.component.UseHandler;
import org.cloudburstmc.api.level.sound.SoundTypes;
import org.cloudburstmc.api.map.MapScale;
import org.cloudburstmc.server.CloudServer;
import org.cloudburstmc.server.map.CloudMapView;
import org.cloudburstmc.server.player.CloudPlayer;

@UtilityClass
public class MapItemHandlers {

    public static final UseHandler USE = MapItemHandlers::create;

    private static ItemStack create(ItemStack item, Entity entity) {
        if (!(entity instanceof CloudPlayer player) || player.isSpectator()) {
            return item;
        }

        CloudMapView map = player.getServer().maps()
                .create(
                        player.getLevel(),
                        player.getPosition().getFloorX(),
                        player.getPosition().getFloorZ(),
                        MapScale.CLOSEST,
                        item.getOrDefault(ItemDataComponents.MAP_TRACKING_POSITION, false)
                );
        map.initializeTerrain(player);

        ItemStack filled = map.createItem();
        player.playSound(SoundTypes.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, 1, 1);
        ItemStack remaining = player.isCreative() ? item : item.decreaseCount();
        if (remaining.isEmpty()) {
            return filled;
        }

        for (ItemStack overflow : player.getContainer().addItem(filled)) {
            player.getLevel().dropItem(player.getPosition(), overflow);
        }

        return remaining;
    }
}
