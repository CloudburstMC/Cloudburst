package org.cloudburstmc.server.command.defaults;

import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.item.ItemKeys;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.item.data.ItemLockMode;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ItemCommandComponentsTest {

    @Test
    void appliesItemProperties() {
        ItemStack stack = apply("""
                {
                  "minecraft:item_lock": {"mode": "lock_in_inventory"},
                  "minecraft:keep_on_death": {}
                }
                """);

        assertEquals(ItemLockMode.LOCK_IN_INVENTORY, stack.get(ItemKeys.ITEM_LOCK));
        assertEquals(Boolean.TRUE, stack.get(ItemKeys.KEEP_ON_DEATH));
    }

    @Test
    void rejectsUnknownComponents() {
        assertThrows(IllegalArgumentException.class, () -> apply("{\"minecraft:unknown\": {}}"));
    }

    @Test
    void resolvesBlockRestrictions() {
        var builder = ItemStack.builder(ItemTypes.DIAMOND);
        ItemCommandComponents.parse("""
                {"minecraft:can_destroy": {"blocks": ["minecraft:stone"]}}
                """, identifier -> Optional.of(BlockTypes.STONE)).applyTo(builder);

        assertEquals(List.of(BlockTypes.STONE), builder.build().get(ItemKeys.CAN_DESTROY));
    }

    @Test
    void rejectsInvalidLockModes() {
        assertThrows(IllegalArgumentException.class, () -> apply("{\"minecraft:item_lock\": {\"mode\": \"unknown\"}}"));
    }

    private static ItemStack apply(String json) {
        var builder = ItemStack.builder(ItemTypes.DIAMOND);
        ItemCommandComponents.parse(json, identifier -> Optional.empty()).applyTo(builder);
        return builder.build();
    }
}
