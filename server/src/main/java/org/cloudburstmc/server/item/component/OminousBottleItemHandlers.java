package org.cloudburstmc.server.item.component;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.event.entity.PotionEffectCause;
import org.cloudburstmc.api.item.ItemDataComponents;
import org.cloudburstmc.api.item.component.FinishUseHandler;
import org.cloudburstmc.api.item.component.UseHandler;
import org.cloudburstmc.api.potion.EffectTypes;
import org.cloudburstmc.api.potion.PotionEffect;
import org.cloudburstmc.protocol.bedrock.data.SoundEvent;
import org.cloudburstmc.server.player.CloudPlayer;

@UtilityClass
public class OminousBottleItemHandlers {

    public static final UseHandler DRINK = ConsumableItemHandlers.USE;

    public static final FinishUseHandler FINISH_DRINK = (item, entity) -> {
        if (!(entity instanceof CloudPlayer player)) {
            return item;
        }

        int amplifier = item.getOrDefault(ItemDataComponents.OMINOUS_BOTTLE_AMPLIFIER, 0);
        player.addPotionEffect(new PotionEffect(EffectTypes.BAD_OMEN, 120_000, amplifier, false, false),
                player, PotionEffectCause.FOOD);
        player.getLevel().addLevelSoundEvent(player.getPosition(), SoundEvent.OMINOUS_BOTTLE_END_USE);
        return player.isCreative() ? item : item.decreaseCount();
    };
}
