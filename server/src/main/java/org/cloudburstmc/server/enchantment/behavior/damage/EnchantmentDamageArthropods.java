package org.cloudburstmc.server.enchantment.behavior.damage;

import org.cloudburstmc.api.enchantment.Enchantment;
import org.cloudburstmc.api.entity.Arthropod;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.potion.EffectTypes;
import org.cloudburstmc.server.potion.CloudEffect;

import java.util.concurrent.ThreadLocalRandom;

public final class EnchantmentDamageArthropods extends EnchantmentDamage {

    @Override
    public float getDamageBonus(Enchantment enchantment, Entity entity) {
        if (entity instanceof Arthropod) {
            return enchantment.level() * 2.5f;
        }

        return 0;
    }

    @Override
    public void doPostAttack(Enchantment enchantment, Entity entity, Entity attacker) {
        if (entity instanceof Arthropod) {
            int duration = 20 + ThreadLocalRandom.current().nextInt(10 * enchantment.level());
            entity.addEffect(new CloudEffect(EffectTypes.SLOWNESS).setDuration(duration).setAmplifier(3));
        }
    }
}
