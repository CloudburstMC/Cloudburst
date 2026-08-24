package org.cloudburstmc.server.enchantment.behavior;

import org.cloudburstmc.api.enchantment.Enchantment;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.damage.DamageSource;
import org.cloudburstmc.api.entity.damage.DamageTypes;
import org.cloudburstmc.api.event.entity.EntityDamageEvent;
import org.cloudburstmc.server.entity.EntityHuman;

import java.util.concurrent.ThreadLocalRandom;

public final class EnchantmentThorns extends EnchantmentBehavior {

    @Override
    public void doPostAttack(Enchantment enchantment, Entity entity, Entity attacker) {
        if (!(entity instanceof EntityHuman)) {
            return;
        }

        ThreadLocalRandom random = ThreadLocalRandom.current();
        if (shouldHit(random, enchantment.level())) {
            DamageSource source = DamageSource.builder(DamageTypes.THORNS)
                    .directEntity(entity).causingEntity(entity).location(entity.getLocation()).build();
            EntityDamageEvent event = new EntityDamageEvent(attacker, source,
                    getDamage(random, enchantment.level()));
            event.setKnockback(0);
            attacker.attack(event);
        }
    }

    private static boolean shouldHit(ThreadLocalRandom random, int level) {
        return level > 0 && random.nextFloat() < 0.15 * level;
    }

    private static int getDamage(ThreadLocalRandom random, int level) {
        return level > 10 ? level - 10 : random.nextInt(1, 5);
    }
}
