package org.cloudburstmc.server.item;

import org.cloudburstmc.api.item.ItemBehaviors;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemType;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.item.component.ConsumableComponent;
import org.cloudburstmc.api.item.component.FoodComponent;
import org.cloudburstmc.server.registry.CloudItemRegistry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FoodBehaviorRegistrationTest {

    private static final CloudItemRegistry ITEMS = CloudItemRegistry.get();

    @Test
    void registersFoodThroughTimedUse() {
        for (ItemType type : new ItemType[]{ItemTypes.APPLE, ItemTypes.COOKED_BEEF, ItemTypes.DRIED_KELP,
                ItemTypes.GOLDEN_APPLE, ItemTypes.HONEY_BOTTLE, ItemTypes.MUSHROOM_STEW, ItemTypes.CHORUS_FRUIT}) {
            assertNotNull(ITEMS.requireComponent(type, ItemBehaviors.FOOD));
            assertNotNull(ITEMS.requireComponent(type, ItemBehaviors.CONSUMABLE));
            assertNotNull(ITEMS.requireComponent(type, ItemBehaviors.USE));
            assertNotNull(ITEMS.requireComponent(type, ItemBehaviors.FINISH_USE));
            assertTrue(ITEMS.requireComponent(type, ItemBehaviors.USE_DURATION_TICKS) > 0);
        }

        assertNull(ITEMS.getComponent(ItemTypes.CAKE, ItemBehaviors.FOOD));
    }

    @Test
    void registersDrinksThroughTheConsumptionPipeline() {
        for (ItemType type : new ItemType[]{ItemTypes.MILK_BUCKET, ItemTypes.POTION, ItemTypes.OMINOUS_BOTTLE}) {
            assertNotNull(ITEMS.requireComponent(type, ItemBehaviors.CONSUMABLE));
            assertNotNull(ITEMS.requireComponent(type, ItemBehaviors.USE));
            assertNotNull(ITEMS.requireComponent(type, ItemBehaviors.FINISH_USE));
            assertTrue(ITEMS.requireComponent(type, ItemBehaviors.USE_DURATION_TICKS) > 0);
        }

        assertEquals(ItemTypes.BUCKET, ITEMS.requireComponent(ItemTypes.MILK_BUCKET, ItemBehaviors.CONSUMABLE).remainder());
        assertEquals(ItemTypes.GLASS_BOTTLE, ITEMS.requireComponent(ItemTypes.POTION, ItemBehaviors.CONSUMABLE).remainder());
        assertNull(ITEMS.requireComponent(ItemTypes.OMINOUS_BOTTLE, ItemBehaviors.CONSUMABLE).remainder());
    }

    @Test
    void storesNutritionSaturationAndRemainders() {
        FoodComponent steak = ITEMS.requireComponent(ItemTypes.COOKED_BEEF, ItemBehaviors.FOOD);
        assertEquals(8, steak.nutrition());
        assertEquals(12.8f, steak.saturation());
        assertFalse(steak.canAlwaysEat());

        FoodComponent apple = ITEMS.requireComponent(ItemTypes.GOLDEN_APPLE, ItemBehaviors.FOOD);
        assertTrue(apple.canAlwaysEat());
        assertEquals(2, ITEMS.requireComponent(ItemTypes.GOLDEN_APPLE, ItemBehaviors.CONSUMABLE).effects().size());

        FoodComponent stew = ITEMS.requireComponent(ItemTypes.RABBIT_STEW, ItemBehaviors.FOOD);
        ConsumableComponent stewUse = ITEMS.requireComponent(ItemTypes.RABBIT_STEW, ItemBehaviors.CONSUMABLE);
        assertEquals(ItemTypes.BOWL, stewUse.remainder());
        assertEquals(12.0f, stew.saturation());
        assertEquals(16, ITEMS.requireComponent(ItemTypes.DRIED_KELP, ItemBehaviors.USE_DURATION_TICKS));
        assertEquals(40, ITEMS.requireComponent(ItemTypes.HONEY_BOTTLE, ItemBehaviors.USE_DURATION_TICKS));
    }

    @Test
    void limitsContainersToTheirSupportedStackSizes() {
        for (ItemType type : new ItemType[]{ItemTypes.BEETROOT_SOUP, ItemTypes.MUSHROOM_STEW,
                ItemTypes.RABBIT_STEW, ItemTypes.SUSPICIOUS_STEW, ItemTypes.MILK_BUCKET, ItemTypes.POTION}) {
            assertEquals(1, ITEMS.requireComponent(type, ItemBehaviors.GET_MAX_STACK_SIZE).execute(ItemStack.from(type)));
        }

        assertEquals(16, ITEMS.requireComponent(ItemTypes.HONEY_BOTTLE, ItemBehaviors.GET_MAX_STACK_SIZE)
                .execute(ItemStack.from(ItemTypes.HONEY_BOTTLE)));
    }
}
