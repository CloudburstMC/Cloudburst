package org.cloudburstmc.server.item.component;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.event.entity.PotionEffectCause;
import org.cloudburstmc.api.item.component.FinishUseHandler;
import org.cloudburstmc.api.item.component.UseHandler;
import org.cloudburstmc.server.player.CloudPlayer;

@UtilityClass
public class MilkBucketItemHandlers {

    public static final UseHandler USE = ConsumableItemHandlers.USE;

    public static final FinishUseHandler FINISH_USE = (item, entity) -> {
        if (!(entity instanceof CloudPlayer player)) {
            return item;
        }

        player.clearActivePotionEffects(PotionEffectCause.MILK);
        return ConsumableItemHandlers.afterConsumption(item, player);
    };
}
