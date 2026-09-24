package org.cloudburstmc.server.item.component;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.cloudburstmc.api.enchantment.EnchantmentTypes;
import org.cloudburstmc.api.entity.EntityTypes;
import org.cloudburstmc.api.entity.misc.FireworksRocket;
import org.cloudburstmc.api.entity.projectile.Arrow;
import org.cloudburstmc.api.entity.projectile.ArrowPickupStatus;
import org.cloudburstmc.api.item.ItemBehaviors;
import org.cloudburstmc.api.item.ItemDataComponents;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.item.component.DamageItemHandler;
import org.cloudburstmc.api.item.component.UseHandler;
import org.cloudburstmc.api.item.component.UseTickHandler;
import org.cloudburstmc.api.item.component.UseTickResult;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.protocol.bedrock.data.SoundEvent;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityEventType;
import org.cloudburstmc.protocol.bedrock.data.inventory.ItemUseType;
import org.cloudburstmc.protocol.bedrock.packet.CompletedUsingItemPacket;
import org.cloudburstmc.protocol.bedrock.packet.EntityEventPacket;
import org.cloudburstmc.server.entity.misc.EntityFireworksRocket;
import org.cloudburstmc.server.player.CloudPlayer;
import org.cloudburstmc.server.registry.CloudEntityRegistry;
import org.cloudburstmc.server.registry.CloudItemRegistry;

import static org.cloudburstmc.server.item.component.ProjectileWeaponSupport.*;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CrossbowItemHandlers {

    private static final int CHARGE_TICKS = 25;
    private static final int QUICK_CHARGE_REDUCTION = 5;
    private static final float MULTISHOT_ANGLE = 10.0f;

    public static final UseHandler USE = (item, entity) -> {
        if (!(entity instanceof CloudPlayer player)) {
            return item;
        }

        ItemStack charged = item.get(ItemDataComponents.CHARGED_PROJECTILE);
        if (charged != null && !charged.isEmpty()) {
            if (player.consumeRecentCompletedItemUse()) {
                player.sendHeldItemSlot();
                return item;
            }

            return fire(item, charged, player);
        }

        if (player.isCreative() || hasAmmunition(player)) {
            player.startUsingItem(item);
        }

        return item;
    };

    public static final UseTickHandler USE_TICK = (item, entity, ticksUsed) -> {
        if (!(entity instanceof CloudPlayer player)) {
            return UseTickResult.continueUsing(item);
        }

        boolean quickCharge = enchantmentLevel(item, EnchantmentTypes.QUICK_CHARGE) > 0;
        if (ticksUsed == Math.max(1, chargeDuration(item) / 5)) {
            player.getLevel().addLevelSoundEvent(player.getPosition(), quickCharge ? SoundEvent.CROSSBOW_QUICK_CHARGE_START : SoundEvent.CROSSBOW_LOADING_START);
        } else if (ticksUsed == chargeDuration(item) / 2) {
            player.getLevel().addLevelSoundEvent(player.getPosition(), quickCharge ? SoundEvent.CROSSBOW_QUICK_CHARGE_MIDDLE : SoundEvent.CROSSBOW_LOADING_MIDDLE);
        }

        if (ticksUsed >= chargeDuration(item) && item.get(ItemDataComponents.CHARGED_PROJECTILE) == null) {
            ItemStack loaded = load(item, player);
            if (loaded != item) {
                EntityEventPacket finished = new EntityEventPacket();
                finished.setType(EntityEventType.FINISHED_CHARGING_ITEM);
                finished.setRuntimeEntityId(player.getRuntimeId());
                player.sendPacket(finished);
                CompletedUsingItemPacket completed = new CompletedUsingItemPacket();
                completed.setItemId(CloudItemRegistry.get().getDefinition(item.getType().getId()).getRuntimeId());
                completed.setType(ItemUseType.UNKNOWN);
                player.sendPacket(completed);
                return UseTickResult.stopUsing(loaded);
            }
        }

        return UseTickResult.continueUsing(item);
    };

    private static ItemStack load(ItemStack item, CloudPlayer player) {
        ItemStack ammunition = loadAmmunition(player);
        if (ammunition.isEmpty()) {
            return item;
        }

        player.getLevel().addLevelSoundEvent(player.getPosition(),
                enchantmentLevel(item, EnchantmentTypes.QUICK_CHARGE) > 0
                        ? SoundEvent.CROSSBOW_QUICK_CHARGE_END : SoundEvent.CROSSBOW_LOADING_END);
        return item.toBuilder().setData(ItemDataComponents.CHARGED_PROJECTILE, ammunition).build();
    }

    private static int chargeDuration(ItemStack item) {
        return CHARGE_TICKS - QUICK_CHARGE_REDUCTION * Math.min(3, enchantmentLevel(item, EnchantmentTypes.QUICK_CHARGE));
    }

    private static boolean hasAmmunition(CloudPlayer player) {
        return player.getOffhand().getOffhandItem().getType() == ItemTypes.FIREWORK_ROCKET || findArrowSlot(player) != NO_AMMO;
    }

    private static ItemStack loadAmmunition(CloudPlayer player) {
        ItemStack offhand = player.getOffhand().getOffhandItem();
        if (offhand.getType() == ItemTypes.FIREWORK_ROCKET) {
            if (!player.isCreative()) {
                player.getOffhand().setOffhandItem(offhand.decreaseCount());
            }

            return offhand.withCount(1);
        }

        int slot = findArrowSlot(player);
        if (slot == NO_AMMO) {
            return player.isCreative() ? ItemStack.from(ItemTypes.ARROW) : ItemStack.EMPTY;
        }

        ItemStack arrow = slot == OFFHAND ? offhand : player.getInventory().getItem(slot);
        if (!player.isCreative()) {
            consumeArrow(player, slot);
        }

        return arrow.withCount(1);
    }

    private static ItemStack fire(ItemStack crossbow, ItemStack ammunition, CloudPlayer player) {
        if (ammunition.getType() != ItemTypes.ARROW && ammunition.getType() != ItemTypes.FIREWORK_ROCKET) {
            return crossbow;
        }

        boolean firework = ammunition.getType() == ItemTypes.FIREWORK_ROCKET;
        boolean multishot = enchantmentLevel(crossbow, EnchantmentTypes.MULTISHOT) > 0;
        if (!shoot(player, ammunition, 0, false)) {
            return crossbow;
        }

        if (multishot) {
            shoot(player, ammunition, -MULTISHOT_ANGLE, true);
            shoot(player, ammunition, MULTISHOT_ANGLE, true);
        }

        player.getLevel().addLevelSoundEvent(player.getPosition(), SoundEvent.CROSSBOW_SHOOT);
        ItemStack unloaded = crossbow.toBuilder().removeData(ItemDataComponents.CHARGED_PROJECTILE).build();
        if (player.isCreative()) {
            return unloaded;
        }

        DamageItemHandler damage = CloudItemRegistry.get().requireComponent(unloaded.getType(), ItemBehaviors.ON_DAMAGE);
        return damage.execute(unloaded, firework ? 3 : 1, player);
    }

    private static boolean shoot(CloudPlayer player, ItemStack ammunition, float yawOffset, boolean sideShot) {
        Vector3f direction = shotDirection(player, yawOffset);
        if (ammunition.getType() == ItemTypes.FIREWORK_ROCKET) {
            FireworksRocket entity = CloudEntityRegistry.get().newEntity(EntityTypes.FIREWORKS_ROCKET,
                    Location.from(player.getPosition().add(0, player.getEyeHeight() - 0.15f, 0), player.getLevel()));
            EntityFireworksRocket rocket = (EntityFireworksRocket) entity;
            rocket.setFireworkData(ammunition.get(ItemDataComponents.FIREWORK_DATA));
            rocket.setOwner(player);
            rocket.setShotAtAngle(true);
            rocket.setMotion(direction.mul(1.6f));
            return rocket.spawn();
        }

        Vector3f motion = direction.mul(3.15f);
        Arrow arrow = player.launchProjectile(EntityTypes.ARROW, motion, projectile -> {
            projectile.setCritical(true);
            projectile.setPickupStatus(player.isCreative() || sideShot ? ArrowPickupStatus.CREATIVE_ONLY : ArrowPickupStatus.ALLOWED);
        });

        return arrow != null;
    }

    private static Vector3f shotDirection(CloudPlayer player, float yawOffset) {
        Vector3f direction = player.getDirectionVector();
        double angle = Math.toRadians(yawOffset);
        double sin = Math.sin(angle);
        double cos = Math.cos(angle);
        return Vector3f.from(direction.getX() * cos - direction.getZ() * sin,
                direction.getY(), direction.getX() * sin + direction.getZ() * cos).normalize();
    }
}
