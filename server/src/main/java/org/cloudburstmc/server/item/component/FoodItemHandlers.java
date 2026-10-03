package org.cloudburstmc.server.item.component;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.event.entity.PotionEffectCause;
import org.cloudburstmc.api.item.ItemBehaviors;
import org.cloudburstmc.api.item.ItemDataComponents;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.component.FinishUseHandler;
import org.cloudburstmc.api.item.component.FoodComponent;
import org.cloudburstmc.api.item.component.UseHandler;
import org.cloudburstmc.api.potion.PotionEffect;
import org.cloudburstmc.server.player.CloudPlayer;
import org.cloudburstmc.server.registry.CloudItemRegistry;

import java.util.List;

@UtilityClass
public class FoodItemHandlers {

    public static final UseHandler USE = (item, entity) -> {
        if (!(entity instanceof CloudPlayer player)) {
            return item;
        }

        FoodComponent food = CloudItemRegistry.get().requireComponent(item.getType(), ItemBehaviors.FOOD);
        if (!player.isCreative() && !food.canAlwaysEat() && player.getFoodData().isFull()) {
            return item;
        }

        return ConsumableItemHandlers.USE.execute(item, entity);
    };

    public static final FinishUseHandler FINISH_USE = (item, entity) -> {
        if (!(entity instanceof CloudPlayer player)) {
            return item;
        }

        consume(item, player);
        return ConsumableItemHandlers.afterConsumption(item, player);
    };

    public static void consume(ItemStack item, CloudPlayer player) {
        FoodComponent food = CloudItemRegistry.get().requireComponent(item.getType(), ItemBehaviors.FOOD);
        player.getFoodData().eat(food.nutrition(), food.saturation(), item);
        for (PotionEffect effect : item.getOrDefault(ItemDataComponents.SUSPICIOUS_STEW_EFFECTS, List.of())) {
            player.addPotionEffect(effect, player, PotionEffectCause.FOOD);
        }
    }
}
