package org.cloudburstmc.server.enchantment.behavior;

import org.cloudburstmc.api.enchantment.Enchantment;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.damage.DamageSource;
import org.cloudburstmc.api.entity.damage.DamageTypes;
import org.cloudburstmc.api.item.ItemBehaviors;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.server.registry.CloudItemRegistry;

import java.util.concurrent.ThreadLocalRandom;

public final class EnchantmentThorns extends EnchantmentBehavior {

    @Override
    public ItemStack onPostHurt(Enchantment enchantment, ItemStack item, Entity wearer, Entity attacker) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        if (!shouldHit(random, enchantment.level())) {
            return item;
        }

        DamageSource source = DamageSource.of(DamageTypes.THORNS, wearer);
        attacker.damage(getDamage(random, enchantment.level()), source);
        return CloudItemRegistry.get().requireComponent(item.getType(), ItemBehaviors.ON_DAMAGE).execute(item, 2, wearer);
    }

    private static boolean shouldHit(ThreadLocalRandom random, int level) {
        return level > 0 && random.nextFloat() < 0.15 * level;
    }

    private static int getDamage(ThreadLocalRandom random, int level) {
        return level > 10 ? level - 10 : random.nextInt(1, 5);
    }
}
