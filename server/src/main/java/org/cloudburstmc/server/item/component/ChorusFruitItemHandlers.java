package org.cloudburstmc.server.item.component;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.item.component.FinishUseHandler;
import org.cloudburstmc.api.item.component.UseHandler;
import org.cloudburstmc.server.player.CloudPlayer;

@UtilityClass
public class ChorusFruitItemHandlers {

    public static final UseHandler USE = (item, entity) -> {
        if (!(entity instanceof CloudPlayer player)
                || player.hasItemCooldown(ItemTypes.CHORUS_FRUIT)) {
            return item;
        }

        return FoodItemHandlers.USE.execute(item, player);
    };

    public static final FinishUseHandler FINISH_USE = (item, entity) -> {
        if (!(entity instanceof CloudPlayer player) || player.hasItemCooldown(ItemTypes.CHORUS_FRUIT)) {
            return item;
        }

        FoodItemHandlers.consume(item, player);
        player.setItemCooldown(ItemTypes.CHORUS_FRUIT, 20);
        ChorusFruitTeleport.teleport(player);
        return ConsumableItemHandlers.afterConsumption(item, player);
    };
}
