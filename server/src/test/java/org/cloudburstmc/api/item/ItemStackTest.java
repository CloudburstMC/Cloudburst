package org.cloudburstmc.api.item;

import org.cloudburstmc.api.util.Identifier;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ItemStackTest {

    @Test
    public void emptyStackUsesAirType() {
        assertSame(ItemTypes.AIR, ItemStack.EMPTY.getType());
        assertSame(ItemStack.EMPTY, ItemStack.EMPTY.toBuilder().build());
    }

    @Test
    public void airFactoriesReturnCanonicalEmptyStack() {
        assertTrue(ItemTypes.AIR.isAir());
        assertFalse(ItemTypes.DIAMOND.isAir());
        assertSame(ItemStack.EMPTY, ItemStack.from(ItemTypes.AIR));

        ItemType equivalentAir = ItemType.of(Identifier.parse("air"));
        assertTrue(equivalentAir.isAir());
        assertSame(ItemStack.EMPTY, ItemStack.from(equivalentAir));
        assertThrows(IllegalArgumentException.class, () -> ItemStack.from(ItemTypes.AIR, 0));
    }

    @Test
    public void airBuilderReturnsCanonicalEmptyStack() {
        assertSame(ItemStack.EMPTY, ItemStack.builder(ItemTypes.AIR).amount(64).build());
    }

    @Test
    public void directionalCountMethodsRejectNegativeAmounts() {
        ItemStack stack = ItemStack.from(ItemTypes.DIAMOND);

        assertThrows(IllegalArgumentException.class, () -> stack.increaseCount(-1));
        assertThrows(IllegalArgumentException.class, () -> stack.decreaseCount(-1));
    }
}
