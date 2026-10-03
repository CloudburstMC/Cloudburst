package org.cloudburstmc.server.item;

import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.server.item.component.DefaultItemHandlers;
import org.cloudburstmc.server.testutil.InterfaceProxy;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemDurabilityTest {

    @Test
    void creativePlayersDoNotLoseItemDurability() {
        Player owner = InterfaceProxy.create(Player.class, Map.of("isAlive", true, "isCreative", true));
        ItemStack sword = ItemStack.builder().itemType(ItemTypes.IRON_SWORD).build().withDamage(20);

        assertEquals(sword, DefaultItemHandlers.ON_DAMAGE.execute(sword, 10, owner));
    }

    @Test
    void largeDamageBreaksTheItemWithoutOverflow() {
        Entity owner = InterfaceProxy.create(Entity.class, Map.of("isAlive", true));
        ItemStack sword = ItemStack.builder().itemType(ItemTypes.IRON_SWORD).build().withDamage(20);

        assertEquals(ItemStack.EMPTY, DefaultItemHandlers.ON_DAMAGE.execute(sword, Integer.MAX_VALUE, owner));
    }

    @Test
    void largeUnbreakingLevelsKeepAPositiveDurabilityChance() {
        assertTrue(DefaultItemHandlers.GET_DAMAGE_CHANCE.execute(Integer.MAX_VALUE) > 0);
    }
}
