package org.cloudburstmc.server.container.cartography;

import org.cloudburstmc.api.item.ItemDataComponents;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemType;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.map.MapScale;
import org.cloudburstmc.server.container.cartography.CartographyResultCalculator.MapState;
import org.cloudburstmc.server.container.cartography.CartographyResultCalculator.Operation;
import org.cloudburstmc.server.container.cartography.CartographyResultCalculator.Result;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CartographyResultCalculatorTest {

    @Test
    void preparesPluginRecipesWithExplicitCostsWithoutChangingStandardRecipes() {
        ItemStack input = ItemStack.builder(ItemTypes.PAPER).amount(4).build();
        ItemStack additional = ItemStack.builder(ItemTypes.GLASS_PANE).amount(3).build();
        ItemStack output = ItemStack.builder(ItemTypes.EMPTY_MAP).amount(2).build();
        Result custom = Result.EMPTY.prepare(output, input, additional);

        assertEquals(Operation.CUSTOM, custom.operation());
        assertEquals(output, custom.item());
        assertEquals(1, custom.inputCount());
        assertEquals(1, custom.additionalCount());
        assertEquals(2, custom.consumedInput(4, output.getCount()));
        assertEquals(2, custom.consumedAdditional(4, output.getCount()));
        assertTrue(custom.canMerge());
        assertFalse(custom.createsMapCopy());
        assertNull(custom.outputMap());

        Result oneInput = Result.EMPTY.prepare(output, input, ItemStack.EMPTY);
        assertEquals(0, oneInput.additionalCount());
        assertSame(Result.EMPTY, Result.EMPTY.prepare(ItemStack.EMPTY, input, additional));

        Result standard = CartographyResultCalculator.calculate(input, ItemStack.EMPTY, null, null);
        assertSame(standard, standard.prepare(output, input, additional));
    }

    @Test
    void derivesPendingMapChangesWithoutChangingTheInputMap() {
        ItemStack input = ItemStack.builder(ItemTypes.FILLED_MAP).setData(ItemDataComponents.MAP_ID, 42L).build();
        MapState original = new MapState(MapScale.CLOSE, false, false);
        Result scaled = CartographyResultCalculator.calculate(input, ItemStack.builder(ItemTypes.PAPER).build(), original, null);
        Result locked = CartographyResultCalculator.calculate(input, ItemStack.builder(ItemTypes.GLASS_PANE).build(), original, null);
        Result locator = CartographyResultCalculator.calculate(input, ItemStack.builder(ItemTypes.COMPASS).build(), original, null);
        Result cloned = CartographyResultCalculator.calculate(input, ItemStack.builder(ItemTypes.EMPTY_MAP).build(), original, null);

        assertEquals(new MapState(MapScale.NORMAL, false, false), scaled.outputMap());
        assertEquals(new MapState(MapScale.CLOSE, true, false), locked.outputMap());
        assertEquals(new MapState(MapScale.CLOSE, false, true), locator.outputMap());
        assertEquals(original, cloned.outputMap());

        for (Result result : new Result[]{scaled, locked, locator, cloned}) {
            assertSame(original, result.map());
            assertEquals(42L, result.item().get(ItemDataComponents.MAP_ID));
        }

        assertNull(Result.EMPTY.outputMap());
    }

    @Test
    void createsEmptyMapsWithExplicitIngredientCosts() {
        ItemStack paper = ItemStack.builder(ItemTypes.PAPER).amount(12).build();
        ItemStack compass = ItemStack.builder(ItemTypes.COMPASS).build();
        Result ordinary = CartographyResultCalculator.calculate(paper, ItemStack.EMPTY, null, null);
        Result locator = CartographyResultCalculator.calculate(paper, compass, null, null);

        assertSame(ItemTypes.EMPTY_MAP, ordinary.item().getType());
        assertEquals(1, ordinary.item().getCount());
        assertFalse(ordinary.item().getOrDefault(ItemDataComponents.MAP_TRACKING_POSITION, false));
        assertEquals(1, ordinary.inputCount());
        assertEquals(0, ordinary.additionalCount());
        assertTrue(ordinary.canMerge());

        assertTrue(locator.item().getOrDefault(ItemDataComponents.MAP_TRACKING_POSITION, false));
        assertEquals(1, locator.inputCount());
        assertEquals(1, locator.additionalCount());
        assertNull(locator.item().get(ItemDataComponents.MAP_ID));

        assertEquals(12, ordinary.consumedInput(12, ordinary.item().getCount()));
        assertEquals(0, ordinary.consumedAdditional(12, ordinary.item().getCount()));
        assertEquals(4, locator.consumedInput(4, locator.item().getCount()));
        assertEquals(4, locator.consumedAdditional(4, locator.item().getCount()));
    }

    @Test
    void convertsNamedEmptyMapsToLocatorMapsWithoutInitializingThem() {
        ItemStack input = ItemStack.builder(ItemTypes.EMPTY_MAP).amount(4).setData(ItemDataComponents.CUSTOM_NAME, "Coast").build();
        ItemStack compass = ItemStack.builder(ItemTypes.COMPASS).build();
        Result result = CartographyResultCalculator.calculate(input, compass, null, null);

        assertEquals(1, result.item().getCount());
        assertEquals("Coast", result.item().get(ItemDataComponents.CUSTOM_NAME));
        assertTrue(result.item().getOrDefault(ItemDataComponents.MAP_TRACKING_POSITION, false));
        assertNull(result.item().get(ItemDataComponents.MAP_ID));
        assertFalse(result.createsMapCopy());
        assertTrue(CartographyResultCalculator.calculate(result.item(), compass, null, null).item().isEmpty());
    }

    @Test
    void renamesMapStacksAndConsumesOnlyTheTakenCount() {
        for (ItemType type : new ItemType[]{ItemTypes.EMPTY_MAP, ItemTypes.FILLED_MAP}) {
            ItemStack input = ItemStack.builder(type).amount(8).build();
            Result result = CartographyResultCalculator.calculate(input, ItemStack.EMPTY, null, "Coast");

            assertEquals(Operation.RENAME, result.operation());
            assertSame(type, result.item().getType());
            assertEquals(8, result.item().getCount());
            assertEquals("Coast", result.item().get(ItemDataComponents.CUSTOM_NAME));
            assertEquals(3, result.consumedInput(3, result.item().getCount()));
            assertEquals(0, result.additionalCount());
        }
    }

    @Test
    void clearsNamesAndRenamesNewlyCreatedMapsRatherThanPaper() {
        ItemStack named = ItemStack.builder(ItemTypes.EMPTY_MAP).setData(ItemDataComponents.CUSTOM_NAME, "Old").build();
        Result cleared = CartographyResultCalculator.calculate(named, ItemStack.EMPTY, null, "");

        assertEquals(Operation.RENAME, cleared.operation());
        assertNull(cleared.item().get(ItemDataComponents.CUSTOM_NAME));
        assertEquals(cleared.item(), CartographyResultCalculator.calculate(cleared.item(), ItemStack.EMPTY, null, "").item());

        Result created = CartographyResultCalculator.calculate(ItemStack.builder(ItemTypes.PAPER).build(), ItemStack.EMPTY, null, "Coast");
        assertEquals(Operation.CREATE_EMPTY, created.operation());
        assertSame(ItemTypes.EMPTY_MAP, created.item().getType());
        assertEquals("Coast", created.item().get(ItemDataComponents.CUSTOM_NAME));
    }

    @Test
    void preparesFilledMapOperationsWithoutAllocatingNewMapIds() {
        ItemStack input = ItemStack.builder(ItemTypes.FILLED_MAP).setData(ItemDataComponents.MAP_ID, 42L).build();
        MapState map = new MapState(MapScale.CLOSEST, false, false);
        Result clone = CartographyResultCalculator.calculate(input, ItemStack.builder(ItemTypes.EMPTY_MAP).build(), map, null);
        Result scale = CartographyResultCalculator.calculate(input, ItemStack.builder(ItemTypes.PAPER).build(), map, null);
        Result lock = CartographyResultCalculator.calculate(input, ItemStack.builder(ItemTypes.GLASS_PANE).build(), map, null);
        Result locator = CartographyResultCalculator.calculate(input, ItemStack.builder(ItemTypes.COMPASS).build(), map, null);

        assertEquals(Operation.CLONE, clone.operation());
        assertEquals(2, clone.item().getCount());
        assertFalse(clone.createsMapCopy());
        assertEquals(3, clone.consumedInput(6, clone.item().getCount()));
        assertEquals(3, clone.consumedAdditional(6, clone.item().getCount()));

        assertEquals(Operation.SCALE, scale.operation());
        assertTrue(scale.createsMapCopy());
        assertEquals(Operation.LOCK, lock.operation());
        assertEquals(Operation.ADD_LOCATOR, locator.operation());
        assertTrue(locator.item().getOrDefault(ItemDataComponents.MAP_TRACKING_POSITION, false));

        for (Result result : new Result[]{clone, scale, lock, locator}) {
            assertEquals(42L, result.item().get(ItemDataComponents.MAP_ID));
            assertEquals(1, result.inputCount());
            assertEquals(1, result.additionalCount());
        }
    }

    @Test
    void permitsTakingUnchangedMapsFromEitherInputSlot() {
        for (ItemType type : new ItemType[]{ItemTypes.EMPTY_MAP, ItemTypes.FILLED_MAP}) {
            ItemStack input = ItemStack.builder(type).amount(8).setData(ItemDataComponents.CUSTOM_NAME, "Coast").build();
            Result upper = CartographyResultCalculator.calculate(input, ItemStack.EMPTY, null, null);
            Result lower = CartographyResultCalculator.calculate(ItemStack.EMPTY, input, null, null);

            assertEquals(Operation.RENAME, upper.operation());
            assertEquals(input, upper.item());
            assertEquals(input, lower.item());
            assertEquals(3, upper.consumedInput(3, upper.item().getCount()));
            assertEquals(0, upper.consumedAdditional(3, upper.item().getCount()));
            assertEquals(0, lower.consumedInput(3, lower.item().getCount()));
            assertEquals(3, lower.consumedAdditional(3, lower.item().getCount()));
        }
    }

    @Test
    void matchesTwoIngredientRecipesInEitherOrder() {
        ItemStack filled = ItemStack.builder(ItemTypes.FILLED_MAP).setData(ItemDataComponents.MAP_ID, 42L).setData(ItemDataComponents.CUSTOM_NAME, "Coast").build();
        MapState map = new MapState(MapScale.CLOSEST, false, false);
        ItemStack paper = ItemStack.builder(ItemTypes.PAPER).build();
        ItemStack compass = ItemStack.builder(ItemTypes.COMPASS).build();
        ItemStack empty = ItemStack.builder(ItemTypes.EMPTY_MAP).build();
        ItemStack glass = ItemStack.builder(ItemTypes.GLASS_PANE).build();
        ItemStack[][] recipes = {
                {filled, paper}, {filled, empty}, {filled, glass}, {filled, compass},
                {paper, compass}, {empty, compass}
        };

        for (ItemStack[] ingredients : recipes) {
            Result forward = CartographyResultCalculator.calculate(ingredients[0], ingredients[1], map, null);
            Result reversed = CartographyResultCalculator.calculate(ingredients[1], ingredients[0], map, null);

            assertFalse(forward.item().isEmpty());
            assertEquals(forward, reversed);
        }
    }

    @Test
    void consumesSingleIngredientRecipesFromTheirActualSlot() {
        ItemStack paper = ItemStack.builder(ItemTypes.PAPER).amount(8).build();
        Result created = CartographyResultCalculator.calculate(ItemStack.EMPTY, paper, null, null);

        assertEquals(Operation.CREATE_EMPTY, created.operation());
        assertEquals(0, created.consumedInput(3, created.item().getCount()));
        assertEquals(3, created.consumedAdditional(3, created.item().getCount()));

        ItemStack maps = ItemStack.builder(ItemTypes.FILLED_MAP).amount(8).setData(ItemDataComponents.MAP_ID, 42L).build();
        Result renamed = CartographyResultCalculator.calculate(ItemStack.EMPTY, maps, null, "Coast");

        assertEquals(Operation.RENAME, renamed.operation());
        assertEquals(8, renamed.item().getCount());
        assertEquals("Coast", renamed.item().get(ItemDataComponents.CUSTOM_NAME));
        assertEquals(0, renamed.consumedInput(3, renamed.item().getCount()));
        assertEquals(3, renamed.consumedAdditional(3, renamed.item().getCount()));
    }

    @Test
    void restrictsLockedMaximumScaleAndMissingMapData() {
        ItemStack input = ItemStack.builder(ItemTypes.FILLED_MAP).setData(ItemDataComponents.MAP_ID, 42L).build();
        ItemStack paper = ItemStack.builder(ItemTypes.PAPER).build();
        ItemStack glass = ItemStack.builder(ItemTypes.GLASS_PANE).build();
        MapState locked = new MapState(MapScale.CLOSEST, true, true);

        assertTrue(CartographyResultCalculator.calculate(input, paper, locked, null).item().isEmpty());
        assertTrue(CartographyResultCalculator.calculate(input, glass, locked, null).item().isEmpty());
        assertTrue(CartographyResultCalculator.calculate(input, paper, new MapState(MapScale.FARTHEST, false, true), null).item().isEmpty());
        assertTrue(CartographyResultCalculator.calculate(paper, input, new MapState(MapScale.FARTHEST, false, true), null).item().isEmpty());
        assertTrue(CartographyResultCalculator.calculate(paper, input, locked, null).item().isEmpty());
        assertTrue(CartographyResultCalculator.calculate(input, paper, null, null).item().isEmpty());
        assertEquals(Operation.CLONE, CartographyResultCalculator.calculate(input, ItemStack.builder(ItemTypes.EMPTY_MAP).build(), locked, null).operation());
        assertTrue(CartographyResultCalculator.calculate(ItemStack.builder(ItemTypes.DIRT).build(), ItemStack.EMPTY, null, "Coast").item().isEmpty());
    }
}
