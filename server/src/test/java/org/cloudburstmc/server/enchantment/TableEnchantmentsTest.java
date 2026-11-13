package org.cloudburstmc.server.enchantment;

import org.cloudburstmc.api.block.BlockStates;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.enchantment.Enchantment;
import org.cloudburstmc.api.enchantment.EnchantmentTypes;
import org.cloudburstmc.api.inventory.view.EnchantmentOffer;
import org.cloudburstmc.api.item.ItemDataComponents;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.block.component.EnchantingTableBlockHandlers;
import org.cloudburstmc.server.testutil.BlockTestRegistry;
import org.cloudburstmc.server.testutil.BlockTestWorld;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

class TableEnchantmentsTest {

    @BeforeAll
    static void initializeBlockTypes() {
        Objects.requireNonNull(BlockTestRegistry.get());
    }

    @Test
    void generatesStableLevelThirtyOffersWithoutTreasureOrConflicts() {
        ItemStack sword = ItemStack.from(ItemTypes.DIAMOND_SWORD);
        List<EnchantmentOffer> offers = TableEnchantments.generate(sword, 15, 36);

        assertEquals(offers, TableEnchantments.generate(sword, 20, 36));
        EnchantmentOffer bottom = Objects.requireNonNull(offers.get(2));
        assertEquals(30, bottom.requiredLevel());

        List<Enchantment> selected = bottom.enchantments();
        for (int i = 0; i < selected.size(); i++) {
            Enchantment enchantment = selected.get(i);
            assertFalse(enchantment.type().treasure());
            assertFalse(enchantment.type().cursed());
            for (int j = i + 1; j < selected.size(); j++) {
                assertFalse(enchantment.type().conflictsWith(selected.get(j).type()));
            }
        }
    }

    @Test
    void rejectsStackedBooksAndAlreadyEnchantedItems() {
        ItemStack book = ItemStack.from(ItemTypes.BOOK, 2);
        Enchantment sharpness = new Enchantment(EnchantmentTypes.SHARPNESS, 1);
        ItemStack sword = ItemStack.from(ItemTypes.DIAMOND_SWORD).toBuilder()
                .setData(ItemDataComponents.ENCHANTMENTS, Map.of(sharpness.type(), sharpness))
                .build();

        assertTrue(TableEnchantments.generate(book, 15, 36).stream().allMatch(Objects::isNull));
        assertTrue(TableEnchantments.generate(sword, 15, 36).stream().allMatch(Objects::isNull));
    }

    @Test
    void countsOnlyBookshelvesWithAnOpenGapAtTheirHeight() {
        BlockTestWorld world = new BlockTestWorld(BlockTestRegistry.get());
        Vector3i table = Vector3i.from(0, 64, 0);
        world.setState(table, BlockStates.ENCHANTING_TABLE);
        world.setState(table.add(2, 0, 0), BlockStates.BOOKSHELF);
        world.setState(table.add(2, 1, 0), BlockStates.BOOKSHELF);
        world.setState(table.add(-2, 0, 0), BlockTypes.CHISELED_BOOKSHELF.getDefaultState());
        world.setState(table.add(1, 0, 0), BlockStates.STONE);

        assertEquals(1, EnchantingTableBlockHandlers.countBookshelves(world.block(table).getLevel(), table));
    }
}
