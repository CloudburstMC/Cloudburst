package org.cloudburstmc.server.item.component;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.cloudburstmc.api.enchantment.EnchantmentTypes;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.EntityTypes;
import org.cloudburstmc.api.entity.projectile.Arrow;
import org.cloudburstmc.api.entity.projectile.ArrowPickupStatus;
import org.cloudburstmc.api.item.ItemBehaviors;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.component.DamageItemHandler;
import org.cloudburstmc.api.item.component.ReleaseUseHandler;
import org.cloudburstmc.api.item.component.UseHandler;
import org.cloudburstmc.protocol.bedrock.data.SoundEvent;
import org.cloudburstmc.server.player.CloudPlayer;
import org.cloudburstmc.server.registry.CloudItemRegistry;

import static org.cloudburstmc.server.item.component.ProjectileWeaponSupport.*;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class BowItemHandlers {

    public static final UseHandler USE = BowItemHandlers::use;
    public static final ReleaseUseHandler RELEASE = BowItemHandlers::release;

    private static ItemStack use(ItemStack item, Entity entity) {
        if (entity instanceof CloudPlayer player && (player.isCreative() || findArrowSlot(player) != NO_AMMO)) {
            player.startUsingItem(item);
        }

        return item;
    }

    private static ItemStack release(ItemStack item, Entity entity, int ticksUsed) {
        if (!(entity instanceof CloudPlayer player)) {
            return item;
        }

        float seconds = ticksUsed / 20.0f;
        float power = Math.min((seconds * seconds + seconds * 2.0f) / 3.0f, 1.0f);
        if (power < 0.1f) {
            return item;
        }

        int slot = findArrowSlot(player);
        if (!player.isCreative() && slot == NO_AMMO) {
            return item;
        }

        int powerLevel = enchantmentLevel(item, EnchantmentTypes.POWER);
        boolean infinite = player.isCreative() || enchantmentLevel(item, EnchantmentTypes.INFINITY) > 0;

        Arrow arrow = player.launchProjectile(EntityTypes.ARROW, ThrowableItemHandlers.launchMotion(player, 3.0f * power, 0), projectile -> {
            projectile.setCritical(power == 1.0f);
            projectile.setDamage(2.0f + (powerLevel == 0 ? 0 : powerLevel * 0.5f + 0.5f));
            projectile.setPickupStatus(infinite ? ArrowPickupStatus.CREATIVE_ONLY : ArrowPickupStatus.ALLOWED);
            if (enchantmentLevel(item, EnchantmentTypes.FLAME) > 0) {
                projectile.setOnFire(5);
            }
        });

        if (arrow == null) {
            return item;
        }

        if (!infinite) {
            consumeArrow(player, slot);
        }

        player.getLevel().addLevelSoundEvent(player.getPosition(), SoundEvent.BOW);
        if (player.isCreative()) {
            return item;
        }

        DamageItemHandler damage = CloudItemRegistry.get().requireComponent(item.getType(), ItemBehaviors.ON_DAMAGE);
        return damage.execute(item, 1, player);
    }
}
