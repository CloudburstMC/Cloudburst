package org.cloudburstmc.server.item.component;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.cloudburstmc.api.entity.EntityTypes;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.item.component.UseHandler;
import org.cloudburstmc.protocol.bedrock.data.SoundEvent;
import org.cloudburstmc.server.player.CloudPlayer;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class WindChargeItemHandlers {

    private static final int COOLDOWN_TICKS = 10;

    public static final UseHandler USE = (item, entity) -> {
        if (!(entity instanceof CloudPlayer player) || player.hasItemCooldown(ItemTypes.WIND_CHARGE)) {
            return item;
        }

        if (player.launchProjectile(EntityTypes.WIND_CHARGE_PROJECTILE,
                ThrowableItemHandlers.launchMotion(player, 1.5f, 0)) == null) {
            return item;
        }

        player.setItemCooldown(ItemTypes.WIND_CHARGE, COOLDOWN_TICKS);
        ThrowableItemHandlers.playThrowSound(player, SoundEvent.THROW);
        return player.isCreative() ? item : item.decreaseCount();
    };
}
