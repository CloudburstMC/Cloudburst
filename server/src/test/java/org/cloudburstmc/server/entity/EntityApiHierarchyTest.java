package org.cloudburstmc.server.entity;

import org.cloudburstmc.api.entity.*;
import org.cloudburstmc.api.entity.misc.ArmorStand;
import org.cloudburstmc.api.entity.misc.DroppedItem;
import org.cloudburstmc.api.player.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EntityApiHierarchyTest {

    @Test
    void damageableIsAnEntityCapability() {
        assertTrue(Entity.class.isAssignableFrom(Damageable.class));
        assertTrue(Damageable.class.isAssignableFrom(LivingEntity.class));
        assertFalse(Damageable.class.isAssignableFrom(DroppedItem.class));
    }

    @Test
    void humansAndArmorStandsAreLivingButNotMobs() {
        assertTrue(LivingEntity.class.isAssignableFrom(Mob.class));
        assertTrue(LivingEntity.class.isAssignableFrom(HumanEntity.class));
        assertTrue(HumanEntity.class.isAssignableFrom(Player.class));
        assertTrue(LivingEntity.class.isAssignableFrom(ArmorStand.class));
        assertFalse(Mob.class.isAssignableFrom(HumanEntity.class));
        assertFalse(Mob.class.isAssignableFrom(ArmorStand.class));
        assertFalse(CloudMob.class.isAssignableFrom(CloudHumanEntity.class));
    }
}
