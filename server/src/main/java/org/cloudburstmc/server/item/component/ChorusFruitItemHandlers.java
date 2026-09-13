package org.cloudburstmc.server.item.component;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.item.component.FinishUseHandler;
import org.cloudburstmc.api.item.component.UseHandler;
import org.cloudburstmc.server.item.food.Food;
import org.cloudburstmc.server.player.CloudPlayer;

@UtilityClass
public class ChorusFruitItemHandlers {

    public static final UseHandler USE = (item, entity) -> {
        if (!(entity instanceof CloudPlayer player)
                || player.hasItemCooldown(ItemTypes.CHORUS_FRUIT)) {
            return item;
        }

        return ConsumableItemHandlers.USE.execute(item, player);
    };

    public static final FinishUseHandler FINISH_USE = (item, entity) -> {
        if (!(entity instanceof CloudPlayer player)
                || player.hasItemCooldown(ItemTypes.CHORUS_FRUIT)
                || !Food.chorus_fruit.eatenBy(player)) {
            return item;
        }

        player.setItemCooldown(ItemTypes.CHORUS_FRUIT, 20);
        return player.isCreative() ? item : item.decreaseCount();
    };
}
