package org.cloudburstmc.server.item.component;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.event.entity.PotionEffectCause;
import org.cloudburstmc.api.item.component.FinishUseHandler;
import org.cloudburstmc.api.potion.EffectTypes;
import org.cloudburstmc.server.player.CloudPlayer;

@UtilityClass
public class HoneyBottleItemHandlers {

    public static final FinishUseHandler FINISH_USE = (item, entity) -> {
        if (!(entity instanceof CloudPlayer player)) {
            return item;
        }

        FoodItemHandlers.consume(item, player);
        player.removePotionEffect(EffectTypes.POISON, PotionEffectCause.FOOD);
        return ConsumableItemHandlers.afterConsumption(item, player);
    };
}
