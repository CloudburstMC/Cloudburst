package org.cloudburstmc.server.item.behavior;

import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.protocol.bedrock.data.SoundEvent;
import lombok.val;
import org.cloudburstmc.server.Server;
import org.cloudburstmc.server.enchantment.EnchantmentInstance;
import org.cloudburstmc.server.enchantment.EnchantmentTypes;
import org.cloudburstmc.server.entity.Entity;
import org.cloudburstmc.server.entity.EntityTypes;
import org.cloudburstmc.server.entity.impl.projectile.EntityArrow;
import org.cloudburstmc.server.entity.projectile.Arrow;
import org.cloudburstmc.server.event.entity.EntityShootBowEvent;
import org.cloudburstmc.server.event.entity.ProjectileLaunchEvent;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.item.data.Damageable;
import org.cloudburstmc.server.level.Location;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.registry.EntityRegistry;

import java.util.Random;

import static org.cloudburstmc.server.item.ItemTypes.ARROW;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class ItemBowBehavior extends ItemToolBehavior {

    public ItemBowBehavior() {
        super(null, null);
    }

    @Override
    public int getMaxDurability() {
        return DURABILITY_BOW;
    }

    @Override
    public int getEnchantAbility(ItemStack item) {
        return 1;
    }

    @Override
    public boolean onClickAir(ItemStack item, Vector3f directionVector, Player player) {
        return player.getInventory().contains(ItemStack.get(ARROW)) || player.isCreative();
    }

    @Override
    public ItemStack onRelease(ItemStack item, int ticksUsed, Player player) {
        ItemStack itemArrow = ItemStack.get(ARROW);

        if ((player.isAdventure() || player.isSurvival()) && !player.getInventory().contains(itemArrow)) {
            player.getInventory().sendContents(player);
            return null;
        }

        float damage = 2;

        EnchantmentInstance bowDamage = item.getEnchantment(EnchantmentTypes.BOW_POWER);
        if (bowDamage != null && bowDamage.getLevel() > 0) {
            damage += 0.25f * (bowDamage.getLevel() + 1);
        }

        EnchantmentInstance flameEnchant = item.getEnchantment(EnchantmentTypes.BOW_FLAME);
        boolean flame = flameEnchant != null && flameEnchant.getLevel() > 0;

        Vector3f position = Vector3f.from(player.getX(), player.getY() + player.getEyeHeight(), player.getZ());
        Vector3f motion = Vector3f.from(
                -Math.sin(player.getYaw() / 180 * Math.PI) * Math.cos(player.getPitch() / 180 * Math.PI),
                -Math.sin(player.getPitch() / 180 * Math.PI),
                Math.cos(player.getYaw() / 180 * Math.PI) * Math.cos(player.getPitch() / 180 * Math.PI)
        );


        double p = (double) ticksUsed / 20;
        double f = Math.min((p * p + p * 2) / 3, 1) * 2;

        Arrow arrow = EntityRegistry.get().newEntity(EntityTypes.ARROW, Location.from(position, player.getLevel()));
        arrow.setPosition(position);
        arrow.setMotion(motion);
        arrow.setRotation((player.getYaw() > 180 ? 360 : 0) - player.getYaw(), -player.getPitch());
        arrow.setOnFire(flame ? 45 * 60 : 0);
        arrow.setDamage(damage);
        arrow.setCritical(f == 2);
        arrow.setOwner(player);

        EntityShootBowEvent entityShootBowEvent = new EntityShootBowEvent(player, item, arrow, f);

        if (f < 0.1 || ticksUsed < 3) {
            entityShootBowEvent.setCancelled();
        }

        Server.getInstance().getEventManager().fire(entityShootBowEvent);
        if (entityShootBowEvent.isCancelled()) {
            entityShootBowEvent.getProjectile().kill();
            player.getInventory().sendContents(player);
        } else {
            entityShootBowEvent.getProjectile().setMotion(entityShootBowEvent.getProjectile().getMotion().mul(entityShootBowEvent.getForce()));
            EnchantmentInstance infinityEnchant = item.getEnchantment(EnchantmentTypes.BOW_INFINITY);
            boolean infinity = infinityEnchant != null && infinityEnchant.getLevel() > 0;
            Entity projectile;
            if (infinity && (projectile = entityShootBowEvent.getProjectile()) instanceof Arrow) {
                ((EntityArrow) projectile).setPickupMode(EntityArrow.PICKUP_CREATIVE);
            }
            if (player.isAdventure() || player.isSurvival()) {
                if (!infinity) {
                    player.getInventory().removeItem(itemArrow);
                }
                if (!this.isUnbreakable(item)) {
                    val dmg = item.getMetadata(Damageable.class);
                    if (dmg != null) {
                        EnchantmentInstance durability = item.getEnchantment(EnchantmentTypes.DURABILITY);
                        if (!(durability != null && durability.getLevel() > 0 && (100 / (durability.getLevel() + 1)) <= new Random().nextInt(100))) {
                            if (dmg.getDurability() + 1 >= getMaxDurability()) {
                                item = item.decrementAmount();
                            } else {
                                item = item.withData(dmg.damage());
                            }
                            player.getInventory().setItemInHand(item);
                        }
                    }
                }
            }
            if (entityShootBowEvent.getProjectile() != null) {
                ProjectileLaunchEvent projectev = new ProjectileLaunchEvent(entityShootBowEvent.getProjectile());
                Server.getInstance().getEventManager().fire(projectev);
                if (projectev.isCancelled()) {
                    entityShootBowEvent.getProjectile().kill();
                } else {
                    entityShootBowEvent.getProjectile().spawnToAll();
                    player.getLevel().addLevelSoundEvent(player.getPosition(), SoundEvent.BOW);
                }
            }
        }

        return null;
    }
}
