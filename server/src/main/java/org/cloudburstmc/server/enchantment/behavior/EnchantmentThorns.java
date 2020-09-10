package org.cloudburstmc.server.enchantment.behavior;

import org.cloudburstmc.server.enchantment.EnchantmentInstance;
import org.cloudburstmc.server.enchantment.EnchantmentTypes;
import org.cloudburstmc.server.entity.Entity;
import org.cloudburstmc.server.entity.impl.Human;
import org.cloudburstmc.server.event.entity.EntityDamageByEntityEvent;
import org.cloudburstmc.server.event.entity.EntityDamageEvent;
import org.cloudburstmc.server.item.ItemStack;

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

    @Override
    public void doPostAttack(EnchantmentInstance enchantment, Entity entity, Entity attacker) {
        if (!(entity instanceof Human)) {
            return;
        }

        Human human = (Human) entity;

        int thornsLevel = 0;

        for (ItemStack armor : human.getInventory().getArmorContents()) {
            EnchantmentInstance thorns = armor.getEnchantment(EnchantmentTypes.THORNS);
            if (thorns != null) {
                thornsLevel = Math.max(thorns.getLevel(), thornsLevel);
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
