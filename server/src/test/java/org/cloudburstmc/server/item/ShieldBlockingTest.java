package org.cloudburstmc.server.item;

import org.cloudburstmc.api.item.ItemBehaviors;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.item.component.AttackBlockingComponent;
import org.cloudburstmc.server.registry.CloudItemRegistry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ShieldBlockingTest {

    @Test
    void blocksOnlyTheForwardHorizontalArc() {
        AttackBlockingComponent blocking = new AttackBlockingComponent(5, 90, 3, 1, 1, 1);
        assertTrue(blocking.blocksDirection(1));
        assertTrue(blocking.blocksDirection(0.1f));
        assertFalse(blocking.blocksDirection(0));
        assertFalse(blocking.blocksDirection(-1));
        assertFalse(blocking.blocksDirection(Float.NaN));
    }

    @Test
    void chargesDurabilityOnlyAboveTheDamageThreshold() {
        AttackBlockingComponent blocking = new AttackBlockingComponent(5, 90, 3, 1, 1, 1);
        assertEquals(0, blocking.durabilityDamage(2.99f));
        assertEquals(4, blocking.durabilityDamage(3));
        assertEquals(4, blocking.durabilityDamage(3.9f));
        assertEquals(11, blocking.durabilityDamage(10));
        assertThrows(IllegalArgumentException.class, () -> blocking.durabilityDamage(-1));
    }

    @Test
    void registersShieldEquipmentAndAxeDisablingForEveryTier() {
        CloudItemRegistry items = CloudItemRegistry.get();
        ItemStack shield = ItemStack.from(ItemTypes.SHIELD);
        assertTrue(items.requireComponent(ItemTypes.SHIELD, ItemBehaviors.ALLOW_OFFHAND).get());
        assertEquals(336, items.requireComponent(ItemTypes.SHIELD, ItemBehaviors.GET_MAX_DAMAGE).execute(shield));
        assertEquals(1, items.requireComponent(ItemTypes.SHIELD, ItemBehaviors.GET_MAX_STACK_SIZE).execute(shield));
        assertEquals(5, items.requireComponent(ItemTypes.SHIELD, ItemBehaviors.BLOCKS_ATTACKS).delayTicks());
        assertNotNull(items.requireComponent(ItemTypes.SHIELD, ItemBehaviors.ON_BREAK));
        ItemTypes.values().forEach(type -> {
            if (type.getId().getName().endsWith("_axe")) {
                assertEquals(5, items.requireComponent(type, ItemBehaviors.GET_BLOCKING_DISABLE_SECONDS).execute(ItemStack.from(type)), type.toString());
            }
        });
        assertEquals(0, items.requireComponent(ItemTypes.DIAMOND_SWORD, ItemBehaviors.GET_BLOCKING_DISABLE_SECONDS).execute(ItemStack.from(ItemTypes.DIAMOND_SWORD)));
    }
}
