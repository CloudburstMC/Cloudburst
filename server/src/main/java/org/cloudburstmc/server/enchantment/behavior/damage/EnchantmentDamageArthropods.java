package org.cloudburstmc.server.enchantment.behavior.damage;

import org.cloudburstmc.api.enchantment.Enchantment;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.EntityComponents;
import org.cloudburstmc.api.entity.LivingEntity;
import org.cloudburstmc.api.event.entity.PotionEffectCause;
import org.cloudburstmc.api.potion.EffectTypes;
import org.cloudburstmc.api.potion.PotionEffect;
import org.cloudburstmc.server.enchantment.behavior.EnchantmentBehavior;
import org.cloudburstmc.server.entity.CloudEntity;
import org.cloudburstmc.server.registry.CloudEntityRegistry;

import java.util.concurrent.ThreadLocalRandom;

public final class EnchantmentDamageArthropods extends EnchantmentBehavior {

    @Override
    public float modifyDamage(Enchantment enchantment, Entity target, float damage) {
        if (isSensitive(target)) {
            return damage + enchantment.level() * 2.5f;
        }

        return damage;
    }

    @Override
    public void onPostAttack(Enchantment enchantment, Entity attacker, Entity target) {
        if (target instanceof CloudEntity entity && target instanceof LivingEntity && isSensitive(target)) {
            int duration = 20 + ThreadLocalRandom.current().nextInt(10 * enchantment.level());
            entity.addPotionEffect(new PotionEffect(EffectTypes.SLOWNESS, duration, 3), attacker, PotionEffectCause.ATTACK);
        }
    }

    private static boolean isSensitive(Entity target) {
        return CloudEntityRegistry.get()
                .requireComponent(target.getType(), EntityComponents.SENSITIVE_TO_BANE_OF_ARTHROPODS)
                .execute(target);
    }
}
