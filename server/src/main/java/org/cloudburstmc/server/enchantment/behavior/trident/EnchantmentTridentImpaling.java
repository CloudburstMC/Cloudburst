package org.cloudburstmc.server.enchantment.behavior.trident;

import org.cloudburstmc.api.enchantment.Enchantment;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.enchantment.behavior.EnchantmentBehavior;
import org.cloudburstmc.server.player.CloudPlayer;

public final class EnchantmentTridentImpaling extends EnchantmentBehavior {

    @Override
    public float modifyDamage(Enchantment enchantment, Entity target, float damage) {
        if ((target instanceof CloudPlayer player && player.isInsideOfWater()) || (target.getLevel().isRaining()
                && target.getLevel().canBlockSeeSky(Vector3i.from(target.getX(), target.getY(), target.getZ())))) {
            return damage + 2.5f * enchantment.level();
        }

        return damage;
    }
}
