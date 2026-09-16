package org.cloudburstmc.server.enchantment.behavior;

import org.cloudburstmc.api.enchantment.Enchantment;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.event.entity.EntityCombustByEntityEvent;
import org.cloudburstmc.server.CloudServer;

public final class EnchantmentFireAspect extends EnchantmentBehavior {

    @Override
    public void onPostAttack(Enchantment enchantment, Entity attacker, Entity target) {
        int duration = Math.max(target.getFireTicks() / 20, enchantment.level() * 4);

        EntityCombustByEntityEvent event = new EntityCombustByEntityEvent(attacker, target, duration);
        CloudServer.getInstance().getEventManager().fire(event);

        if (!event.isCancelled()) {
            target.setOnFire(event.getDuration());
        }
    }
}
