package org.cloudburstmc.server.enchantment.behavior;

import org.cloudburstmc.api.enchantment.Enchantment;
import org.cloudburstmc.api.enchantment.EnchantmentInstance;
import org.cloudburstmc.api.enchantment.EnchantmentTypes;
import org.cloudburstmc.api.enchantment.behavior.EnchantmentBehavior;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.event.entity.EntityDamageByEntityEvent;
import org.cloudburstmc.api.event.entity.EntityDamageEvent;
import org.cloudburstmc.api.item.ItemKeys;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.server.entity.EntityHuman;

import java.util.concurrent.ThreadLocalRandom;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class EnchantmentThorns extends EnchantmentBehavior {

    @Override
    public int getMinEnchantAbility(int level) {
        return 10 + (level - 1) * 20;
    }

    @Override
    public int getMaxEnchantAbility(int level) {
        return this.getMinEnchantAbility(level) + 50;
    }

    //@Override
    public void doPostAttack(EnchantmentInstance enchantment, Entity entity, Entity attacker) {
        if (!(entity instanceof EntityHuman human)) {
            return;
        }

        int thornsLevel = 0;

        for (ItemStack armor : human.getInventory().getArmorContents()) {
            Enchantment thorns = armor.get(ItemKeys.ENCHANTMENTS).getOrDefault(EnchantmentTypes.THORNS, null);
            if (thorns != null) {
                thornsLevel = Math.max(thorns.level(), thornsLevel);
            }
        }

        ThreadLocalRandom random = ThreadLocalRandom.current();

        if (shouldHit(random, thornsLevel)) {
            attacker.attack(new EntityDamageByEntityEvent(entity, attacker, EntityDamageEvent.DamageCause.ENTITY_ATTACK, getDamage(random, enchantment.getLevel()), 0f));
        }
    }

    private static boolean shouldHit(ThreadLocalRandom random, int level) {
        return level > 0 && random.nextFloat() < 0.15 * level;
    }

    private static int getDamage(ThreadLocalRandom random, int level) {
        return level > 10 ? level - 10 : random.nextInt(1, 5);
    }
}
