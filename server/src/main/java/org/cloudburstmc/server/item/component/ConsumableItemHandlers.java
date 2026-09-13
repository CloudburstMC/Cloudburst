package org.cloudburstmc.server.item.component;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.item.ItemComponents;
import org.cloudburstmc.api.item.component.UseHandler;
import org.cloudburstmc.server.player.CloudPlayer;
import org.cloudburstmc.server.registry.CloudItemRegistry;

@UtilityClass
public class ConsumableItemHandlers {

    public static final UseHandler USE = (item, entity) -> {
        if (!(entity instanceof CloudPlayer player)) {
            return item;
        }

        int duration = CloudItemRegistry.get().requireComponent(item.getType(), ItemComponents.USE_DURATION_TICKS);
        player.startUsingItem(item, duration);
        return item;
    };
}
