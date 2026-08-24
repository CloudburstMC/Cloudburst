package org.cloudburstmc.server.enchantment.behavior.trident;

import org.cloudburstmc.api.enchantment.Enchantment;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.server.enchantment.behavior.EnchantmentBehavior;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.player.CloudPlayer;

public final class EnchantmentTridentImpaling extends EnchantmentBehavior {

    @Override
    public float getDamageBonus(Enchantment enchantment, Entity entity) {
        if ((entity instanceof CloudPlayer && ((CloudPlayer) entity).isInsideOfWater()) || (entity.getLevel().isRaining() && entity.getLevel().canBlockSeeSky(Vector3i.from(entity.getX(), entity.getY(), entity.getZ())))) {
            return 2.5f * enchantment.level();
        }

        return 0f;
    }

}
