package org.cloudburstmc.server.enchantment.behavior.damage;

import org.cloudburstmc.server.enchantment.EnchantmentInstance;
import org.cloudburstmc.server.entity.Arthropod;
import org.cloudburstmc.server.entity.Entity;
import org.cloudburstmc.server.potion.Effect;

import java.util.concurrent.ThreadLocalRandom;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class EnchantmentDamageArthropods extends EnchantmentDamage {

    public EnchantmentDamageArthropods() {
        super(TYPE.SMITE);
    }

    @Override
    public int getMinEnchantAbility(int level) {
        return 5 + (level - 1) * 8;
    }

    @Override
    public int getMaxEnchantAbility(int level) {
        return this.getMinEnchantAbility(level) + 20;
    }

    @Override
    public float getDamageBonus(EnchantmentInstance enchantment, Entity entity) {
        if (entity instanceof Arthropod) {
            return enchantment.getLevel() * 2.5f;
        }

        return 0;
    }

    @Override
    public void doPostAttack(EnchantmentInstance enchantment, Entity entity, Entity attacker) {
        if (entity instanceof Arthropod) {
            int duration = 20 + ThreadLocalRandom.current().nextInt(10 * enchantment.getLevel());
            entity.addEffect(Effect.getEffect(Effect.SLOWNESS).setDuration(duration).setAmplifier(3));
        }
    }
}
