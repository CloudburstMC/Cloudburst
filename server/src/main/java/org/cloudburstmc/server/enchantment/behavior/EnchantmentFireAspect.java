package org.cloudburstmc.server.enchantment.behavior;

import org.cloudburstmc.api.enchantment.Enchantment;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.event.entity.EntityCombustByEntityEvent;
import org.cloudburstmc.server.CloudServer;

public final class EnchantmentFireAspect extends EnchantmentBehavior {

    @Override
    public void doPostAttack(Enchantment enchantment, Entity entity, Entity attacker) {
        int duration = Math.max(entity.getFireTicks() / 20, enchantment.level() * 4);

        EntityCombustByEntityEvent ev = new EntityCombustByEntityEvent(attacker, entity, duration);
        CloudServer.getInstance().getEventManager().fire(ev);

        if (!ev.isCancelled()) {
            entity.setOnFire(ev.getDuration());
        }
    }
}
