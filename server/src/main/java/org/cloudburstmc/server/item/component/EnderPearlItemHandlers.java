package org.cloudburstmc.server.item.component;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.entity.EntityTypes;
import org.cloudburstmc.api.entity.projectile.EnderPearl;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.item.component.UseHandler;
import org.cloudburstmc.protocol.bedrock.data.SoundEvent;
import org.cloudburstmc.server.player.CloudPlayer;

@UtilityClass
public class EnderPearlItemHandlers {

    private static final int COOLDOWN_TICKS = 20;

    public static final UseHandler USE = (item, entity) -> {
        if (!(entity instanceof CloudPlayer player) || player.hasItemCooldown(ItemTypes.ENDER_PEARL)) {
            return item;
        }

        EnderPearl pearl = player.launchProjectile(EntityTypes.ENDER_PEARL, ThrowableItemHandlers.launchMotion(player, 1.5f, 0));
        if (pearl == null) {
            return item;
        }

        player.setItemCooldown(ItemTypes.ENDER_PEARL, COOLDOWN_TICKS);
        ThrowableItemHandlers.playThrowSound(player, SoundEvent.THROW);
        return player.isCreative() ? item : item.decreaseCount();
    };
}
