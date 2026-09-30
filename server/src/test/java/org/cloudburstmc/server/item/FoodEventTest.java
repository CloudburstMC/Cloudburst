package org.cloudburstmc.server.item;

import org.cloudburstmc.api.event.player.PlayerExhaustionEvent;
import org.cloudburstmc.api.event.player.PlayerFoodLevelChangeEvent;
import org.cloudburstmc.api.event.player.PlayerItemConsumeEvent;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.player.ExhaustionReason;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.server.item.component.ConsumableItemHandlers;
import org.cloudburstmc.server.testutil.InterfaceProxy;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FoodEventTest {

    private final Player player = InterfaceProxy.create(Player.class);

    @Test
    void consumedItemAndInventoryReplacementAreIndependent() {
        ItemStack apple = ItemStack.from(ItemTypes.APPLE);
        ItemStack replacement = ItemStack.from(ItemTypes.BOWL);
        PlayerItemConsumeEvent event = new PlayerItemConsumeEvent(player, apple);

        assertSame(apple, event.getItem());
        assertNull(event.getReplacement());
        assertSame(ItemStack.EMPTY, ConsumableItemHandlers.result(event, ItemStack.EMPTY));

        ItemStack potion = ItemStack.from(ItemTypes.POTION);
        event.setItem(potion);
        assertSame(potion, event.getItem());
        assertNull(event.getReplacement());

        event.setReplacement(replacement);
        assertSame(potion, event.getItem());
        assertSame(replacement, ConsumableItemHandlers.result(event, ItemStack.EMPTY));
        event.setReplacement(null);
        assertSame(ItemStack.EMPTY, ConsumableItemHandlers.result(event, ItemStack.EMPTY));

        event.setItem(ItemStack.EMPTY);
        assertSame(ItemStack.EMPTY, event.getItem());
        assertThrows(NullPointerException.class, () -> event.setItem(null));
    }

    @Test
    void foodChangeIdentifiesItsSource() {
        ItemStack food = ItemStack.from(ItemTypes.APPLE);
        PlayerFoodLevelChangeEvent event = new PlayerFoodLevelChangeEvent(player, 20, 6.0f, food);

        assertSame(food, event.getItem());
        event.setFoodLevel(18);
        event.setSaturation(4.0f);
        assertEquals(18, event.getFoodLevel());
        assertEquals(4.0f, event.getSaturation());
    }

    @Test
    void exhaustionReportsReasonAndMutableAmount() {
        PlayerExhaustionEvent event = new PlayerExhaustionEvent(player, ExhaustionReason.BLOCK_MINED, 0.005f);

        assertEquals(ExhaustionReason.BLOCK_MINED, event.getReason());
        event.setAmount(0.01f);
        assertEquals(0.01f, event.getAmount());
        assertThrows(IllegalArgumentException.class, () -> event.setAmount(Float.NaN));
    }
}
