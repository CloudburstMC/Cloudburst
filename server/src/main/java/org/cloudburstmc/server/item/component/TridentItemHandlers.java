package org.cloudburstmc.server.item.component;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.cloudburstmc.api.enchantment.EnchantmentTypes;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.EntityTypes;
import org.cloudburstmc.api.entity.projectile.ArrowPickupStatus;
import org.cloudburstmc.api.entity.projectile.ThrownTrident;
import org.cloudburstmc.api.item.ItemBehaviors;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.component.DamageItemHandler;
import org.cloudburstmc.api.item.component.ReleaseUseHandler;
import org.cloudburstmc.api.item.component.UseHandler;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.protocol.bedrock.data.SoundEvent;
import org.cloudburstmc.server.player.CloudPlayer;
import org.cloudburstmc.server.registry.CloudItemRegistry;

import static org.cloudburstmc.server.item.component.ProjectileWeaponSupport.enchantmentLevel;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class TridentItemHandlers {

    private static final int THROW_THRESHOLD_TICKS = 10;

    public static final UseHandler USE = (item, entity) -> {
        if (entity instanceof CloudPlayer player && canUse(item, player)) {
            player.startUsingItem(item);
        }

        return item;
    };

    public static final ReleaseUseHandler RELEASE = (item, entity, ticksUsed) -> {
        if (!(entity instanceof CloudPlayer player) || ticksUsed < THROW_THRESHOLD_TICKS || !canUse(item, player)) {
            return item;
        }

        int riptide = enchantmentLevel(item, EnchantmentTypes.RIPTIDE);
        if (riptide > 0) {
            Vector3f motion = player.getDirectionVector().mul(1.5f + riptide);
            player.setMotion(motion);
            player.startSpinAttack(20);
            player.getLevel().addLevelSoundEvent(player.getPosition(), switch (Math.min(3, riptide)) {
                case 1 -> SoundEvent.ITEM_TRIDENT_RIPTIDE_1;
                case 2 -> SoundEvent.ITEM_TRIDENT_RIPTIDE_2;
                default -> SoundEvent.ITEM_TRIDENT_RIPTIDE_3;
            });

            return damage(item, player);
        }

        Vector3f motion = ThrowableItemHandlers.launchMotion(player, 2.5f, 0);
        ItemStack thrown = player.isCreative() ? item.withCount(1) : damage(item, player).withCount(1);
        if (thrown.isEmpty()) {
            return item;
        }

        ThrownTrident projectile = player.launchProjectile(EntityTypes.THROWN_TRIDENT, motion, trident -> {
            trident.setTrident(thrown);
            trident.setPickupStatus(player.isCreative() ? ArrowPickupStatus.CREATIVE_ONLY : ArrowPickupStatus.ALLOWED);
        });

        if (projectile == null) {
            return item;
        }

        ThrowableItemHandlers.playThrowSound(player, SoundEvent.ITEM_TRIDENT_THROW);
        return player.isCreative() ? item : item.decreaseCount();
    };

    private static boolean canUse(ItemStack item, CloudPlayer player) {
        int maxDamage = CloudItemRegistry.get().requireComponent(item.getType(), ItemBehaviors.GET_MAX_DAMAGE).execute(item);
        if (!player.isCreative() && maxDamage - item.getDamage() <= 1) {
            return false;
        }

        if (enchantmentLevel(item, EnchantmentTypes.RIPTIDE) == 0) {
            return true;
        }

        return player.isInsideOfWater() || (player.getLevel().isRaining() && player.getLevel().canBlockSeeSky(player.getPosition()));
    }

    private static ItemStack damage(ItemStack item, Entity owner) {
        if (owner instanceof CloudPlayer player && player.isCreative()) {
            return item;
        }

        DamageItemHandler handler = CloudItemRegistry.get().requireComponent(item.getType(), ItemBehaviors.ON_DAMAGE);
        return handler.execute(item, 1, owner);
    }
}
