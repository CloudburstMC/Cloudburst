package org.cloudburstmc.server.enchantment.behavior;

import org.cloudburstmc.server.enchantment.EnchantmentInstance;
import org.cloudburstmc.server.entity.Entity;
import org.cloudburstmc.server.event.entity.EntityCombustByEntityEvent;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class EnchantmentFireAspect extends EnchantmentBehavior {

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
        int duration = Math.max(entity.getFireTicks() / 20, enchantment.getLevel() * 4);

        EntityCombustByEntityEvent ev = new EntityCombustByEntityEvent(attacker, entity, duration);

        if (!ev.isCancelled())
            entity.setOnFire(ev.getDuration());
    }
}
