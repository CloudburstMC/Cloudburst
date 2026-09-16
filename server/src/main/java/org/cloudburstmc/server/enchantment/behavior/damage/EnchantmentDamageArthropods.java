package org.cloudburstmc.server.enchantment.behavior.damage;

import org.cloudburstmc.api.enchantment.Enchantment;
import org.cloudburstmc.api.entity.Arthropod;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.potion.EffectTypes;
import org.cloudburstmc.server.enchantment.behavior.EnchantmentBehavior;
import org.cloudburstmc.server.potion.CloudEffect;

import java.util.concurrent.ThreadLocalRandom;

public final class EnchantmentDamageArthropods extends EnchantmentBehavior {

    @Override
    public float modifyDamage(Enchantment enchantment, Entity target, float damage) {
        if (target instanceof Arthropod) {
            return damage + enchantment.level() * 2.5f;
        }

        return damage;
    }

    @Override
    public void onPostAttack(Enchantment enchantment, Entity attacker, Entity target) {
        if (target instanceof Arthropod) {
            int duration = 20 + ThreadLocalRandom.current().nextInt(10 * enchantment.level());
            target.addEffect(new CloudEffect(EffectTypes.SLOWNESS).setDuration(duration).setAmplifier(3));
        }
    }
}
