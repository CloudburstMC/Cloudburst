package org.cloudburstmc.server.enchantment.behavior.trident;

import org.cloudburstmc.api.enchantment.EnchantmentInstance;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.player.CloudPlayer;

public class EnchantmentTridentImpaling extends EnchantmentTrident {

    @Override
    public int getMinEnchantAbility(int level) {
        return 1 + (level - 1) * 10;
    }

    @Override
    public int getMaxEnchantAbility(int level) {
        return this.getMinEnchantAbility(level) + 15;
    }

    @Override
    public float getDamageBonus(EnchantmentInstance enchantment, Entity entity) {
        if ((entity instanceof CloudPlayer && ((CloudPlayer) entity).isInsideOfWater()) || (entity.getLevel().isRaining() && entity.getLevel().canBlockSeeSky(Vector3i.from(entity.getX(), entity.getY(), entity.getZ())))) {
            return 2.5f * enchantment.getLevel();
        }

        return 0f;
    }

}
