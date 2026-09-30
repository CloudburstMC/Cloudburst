package org.cloudburstmc.server.entity;

import org.cloudburstmc.api.entity.Living;
import org.cloudburstmc.api.entity.damage.DamageSource;
import org.cloudburstmc.api.entity.damage.DamageTypes;
import org.cloudburstmc.api.event.entity.EntityDeathEvent;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.server.testutil.InterfaceProxy;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DeathEventTest {

    @Test
    void editsDropsIndependentlyAndExposesActualExperienceAmounts() {
        ItemStack drop = ItemStack.builder().itemType(ItemTypes.DIAMOND).build();
        List<ItemStack> inventoryDrops = new ArrayList<>(List.of(drop));
        EntityDeathEvent event = new EntityDeathEvent(
                InterfaceProxy.create(Living.class, Map.of("getMaxHealth", 20)),
                DamageSource.of(DamageTypes.GENERIC_KILL), inventoryDrops, 7
        );

        event.getDrops().clear();
        event.setDroppedExperience(12);
        assertEquals(List.of(drop), inventoryDrops);
        assertEquals(List.of(), event.getDrops());
        assertEquals(12, event.getDroppedExperience());
        assertThrows(IllegalArgumentException.class, () -> event.setDroppedExperience(-1));
    }

    @Test
    void revivalHealthMustKeepTheEntityAliveWithinItsMaximum() {
        EntityDeathEvent event = new EntityDeathEvent(
                InterfaceProxy.create(Living.class, Map.of("getMaxHealth", 20)),
                DamageSource.of(DamageTypes.GENERIC_KILL), List.of(), 0
        );

        assertEquals(20, event.getReviveHealth());
        event.setReviveHealth(0.5f);
        assertEquals(0.5f, event.getReviveHealth());
        assertThrows(IllegalArgumentException.class, () -> event.setReviveHealth(0));
        assertThrows(IllegalArgumentException.class, () -> event.setReviveHealth(21));
        assertThrows(IllegalArgumentException.class, () -> event.setReviveHealth(Float.NaN));
    }
}
