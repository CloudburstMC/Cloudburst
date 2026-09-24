package org.cloudburstmc.server.item.component;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.EntityTypes;
import org.cloudburstmc.api.entity.damage.DamageSource;
import org.cloudburstmc.api.entity.damage.DamageTypes;
import org.cloudburstmc.api.entity.projectile.SplashPotion;
import org.cloudburstmc.api.event.entity.PotionEffectCause;
import org.cloudburstmc.api.item.ItemDataComponents;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.item.component.FinishUseHandler;
import org.cloudburstmc.api.item.component.UseHandler;
import org.cloudburstmc.api.potion.PotionType;
import org.cloudburstmc.api.potion.PotionTypes;
import org.cloudburstmc.protocol.bedrock.data.SoundEvent;
import org.cloudburstmc.server.player.CloudPlayer;
import org.cloudburstmc.server.potion.CloudPotion;

@UtilityClass
public class PotionItemHandlers {

    public static final UseHandler DRINK = ConsumableItemHandlers.USE;

    public static final FinishUseHandler FINISH_DRINK = (item, entity) -> {
        if (!(entity instanceof CloudPlayer player)) {
            return item;
        }

        PotionType type = item.getOrDefault(ItemDataComponents.POTION_TYPE, PotionTypes.WATER);
        DamageSource source = DamageSource.of(DamageTypes.MAGIC, player);
        new CloudPotion(type).apply(player, 1.0, source, player, PotionEffectCause.POTION_DRINK);
        player.getLevel().addLevelSoundEvent(player.getPosition(), SoundEvent.DRINK);
        return player.isCreative() ? item : ItemStack.from(ItemTypes.GLASS_BOTTLE);
    };

    public static final UseHandler THROW_SPLASH = (item, entity) -> throwPotion(item, entity, EntityTypes.SPLASH_POTION);

    public static final UseHandler THROW_LINGERING = (item, entity) -> throwPotion(item, entity, EntityTypes.LINGERING_POTION);

    private static ItemStack throwPotion(ItemStack item, Entity entity, EntityType<? extends SplashPotion> type) {
        if (!(entity instanceof CloudPlayer player)) {
            return item;
        }

        PotionType potionType = item.getOrDefault(ItemDataComponents.POTION_TYPE, PotionTypes.WATER);
        SplashPotion projectile = player.launchProjectile(type, ThrowableItemHandlers.launchMotion(player, 0.5f, -20),
                potion -> potion.setPotionType(potionType));
        if (projectile == null) {
            return item;
        }

        ThrowableItemHandlers.playThrowSound(player, SoundEvent.THROW);
        return player.isCreative() ? item : item.decreaseCount();
    }
}
