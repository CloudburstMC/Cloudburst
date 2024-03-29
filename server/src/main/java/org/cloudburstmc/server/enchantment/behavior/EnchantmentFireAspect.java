package org.cloudburstmc.server.enchantment.behavior;

import org.cloudburstmc.api.enchantment.EnchantmentInstance;
import org.cloudburstmc.api.enchantment.behavior.EnchantmentBehavior;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.event.entity.EntityCombustByEntityEvent;
import org.cloudburstmc.server.CloudServer;

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
        CloudServer.getInstance().getEventManager().fire(ev);

        if (!ev.isCancelled()) {
            entity.setOnFire(ev.getDuration());
        }
    }
}
