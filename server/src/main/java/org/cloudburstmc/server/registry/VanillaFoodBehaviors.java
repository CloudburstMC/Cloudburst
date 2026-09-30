package org.cloudburstmc.server.registry;

import lombok.experimental.UtilityClass;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.item.ItemBehaviors;
import org.cloudburstmc.api.item.ItemType;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.item.component.ConsumableComponent;
import org.cloudburstmc.api.item.component.FoodComponent;
import org.cloudburstmc.api.level.sound.SoundTypes;
import org.cloudburstmc.api.potion.EffectTypes;
import org.cloudburstmc.api.potion.PotionEffect;
import org.cloudburstmc.server.item.component.ChorusFruitItemHandlers;
import org.cloudburstmc.server.item.component.FoodItemHandlers;
import org.cloudburstmc.server.item.component.HoneyBottleItemHandlers;
import org.cloudburstmc.server.item.serializer.SuspiciousStewItemSerializer;

import java.util.List;

@UtilityClass
public class VanillaFoodBehaviors {

    public static void configure(CloudItemRegistry registry) {
        food(registry, ItemTypes.APPLE, 4, 0.3f);
        food(registry, ItemTypes.BAKED_POTATO, 5, 0.6f);
        food(registry, ItemTypes.BEEF, 3, 0.3f);
        food(registry, ItemTypes.BEETROOT, 1, 0.6f);
        stew(registry, ItemTypes.BEETROOT_SOUP, 6);
        food(registry, ItemTypes.BREAD, 5, 0.6f);
        food(registry, ItemTypes.CARROT, 3, 0.6f);
        food(registry, ItemTypes.CHICKEN, 2, 0.3f, new ConsumableComponent.Effect(new PotionEffect(EffectTypes.HUNGER, 600, 0), 0.3f));
        food(registry, ItemTypes.CHORUS_FRUIT, 4, 0.3f, true, null);
        registry.configure(ItemTypes.CHORUS_FRUIT)
                .set(ItemBehaviors.USE, ChorusFruitItemHandlers.USE)
                .set(ItemBehaviors.FINISH_USE, ChorusFruitItemHandlers.FINISH_USE);
        food(registry, ItemTypes.COD, 2, 0.1f);
        food(registry, ItemTypes.COOKED_BEEF, 8, 0.8f);
        food(registry, ItemTypes.COOKED_CHICKEN, 6, 0.6f);
        food(registry, ItemTypes.COOKED_COD, 5, 0.6f);
        food(registry, ItemTypes.COOKED_MUTTON, 6, 0.8f);
        food(registry, ItemTypes.COOKED_PORKCHOP, 8, 0.8f);
        food(registry, ItemTypes.COOKED_RABBIT, 5, 0.6f);
        food(registry, ItemTypes.COOKED_SALMON, 6, 0.8f);
        food(registry, ItemTypes.COOKIE, 2, 0.1f);
        food(registry, ItemTypes.DRIED_KELP, 1, 0.3f, false, null, 16);
        food(registry, ItemTypes.ENCHANTED_GOLDEN_APPLE, 4, 1.2f, true, null,
                new ConsumableComponent.Effect(new PotionEffect(EffectTypes.REGENERATION, 400, 1), 1),
                new ConsumableComponent.Effect(new PotionEffect(EffectTypes.ABSORPTION, 2400, 3), 1),
                new ConsumableComponent.Effect(new PotionEffect(EffectTypes.RESISTANCE, 6000, 0), 1),
                new ConsumableComponent.Effect(new PotionEffect(EffectTypes.FIRE_RESISTANCE, 6000, 0), 1));
        food(registry, ItemTypes.GLOW_BERRIES, 2, 0.1f);
        food(registry, ItemTypes.GOLDEN_APPLE, 4, 1.2f, true, null,
                new ConsumableComponent.Effect(new PotionEffect(EffectTypes.REGENERATION, 100, 1), 1),
                new ConsumableComponent.Effect(new PotionEffect(EffectTypes.ABSORPTION, 2400, 0), 1));
        food(registry, ItemTypes.GOLDEN_CARROT, 6, 1.2f);
        food(registry, ItemTypes.HONEY_BOTTLE, 6, 0.1f, true, ItemTypes.GLASS_BOTTLE, 40);
        registry.configure(ItemTypes.HONEY_BOTTLE)
                .set(ItemBehaviors.GET_MAX_STACK_SIZE, item -> 16)
                .set(ItemBehaviors.CONSUMABLE, new ConsumableComponent(ItemTypes.GLASS_BOTTLE,
                        SoundTypes.RANDOM_DRINK_HONEY, List.of()))
                .set(ItemBehaviors.FINISH_USE, HoneyBottleItemHandlers.FINISH_USE);
        food(registry, ItemTypes.MELON_SLICE, 2, 0.3f);
        stew(registry, ItemTypes.MUSHROOM_STEW, 6);
        food(registry, ItemTypes.MUTTON, 2, 0.3f);
        food(registry, ItemTypes.POISONOUS_POTATO, 2, 0.3f,
                new ConsumableComponent.Effect(new PotionEffect(EffectTypes.POISON, 100, 0), 0.6f));
        food(registry, ItemTypes.PORKCHOP, 3, 0.3f);
        food(registry, ItemTypes.POTATO, 1, 0.3f);
        food(registry, ItemTypes.PUFFERFISH, 1, 0.1f,
                new ConsumableComponent.Effect(new PotionEffect(EffectTypes.HUNGER, 300, 2), 1),
                new ConsumableComponent.Effect(new PotionEffect(EffectTypes.NAUSEA, 300, 0), 1),
                new ConsumableComponent.Effect(new PotionEffect(EffectTypes.POISON, 1200, 1), 1));
        food(registry, ItemTypes.PUMPKIN_PIE, 8, 0.3f);
        food(registry, ItemTypes.RABBIT, 3, 0.3f);
        stew(registry, ItemTypes.RABBIT_STEW, 10);
        food(registry, ItemTypes.ROTTEN_FLESH, 4, 0.1f,
                new ConsumableComponent.Effect(new PotionEffect(EffectTypes.HUNGER, 600, 0), 0.8f));
        food(registry, ItemTypes.SALMON, 2, 0.1f);
        food(registry, ItemTypes.SPIDER_EYE, 2, 0.8f,
                new ConsumableComponent.Effect(new PotionEffect(EffectTypes.POISON, 100, 0), 1));
        food(registry, ItemTypes.SUSPICIOUS_STEW, 6, 0.6f, true, ItemTypes.BOWL);
        registry.configure(ItemTypes.SUSPICIOUS_STEW, new SuspiciousStewItemSerializer())
                .set(ItemBehaviors.GET_MAX_STACK_SIZE, item -> 1);
        food(registry, ItemTypes.SWEET_BERRIES, 2, 0.1f);
        food(registry, ItemTypes.TROPICAL_FISH, 1, 0.1f);
    }

    private static void stew(CloudItemRegistry registry, ItemType type, int nutrition) {
        food(registry, type, nutrition, 0.6f, false, ItemTypes.BOWL);
        registry.configure(type).set(ItemBehaviors.GET_MAX_STACK_SIZE, item -> 1);
    }

    private static void food(CloudItemRegistry registry, ItemType type, int nutrition, float modifier, ConsumableComponent.Effect... effects) {
        food(registry, type, nutrition, modifier, false, null, 32, effects);
    }

    private static void food(CloudItemRegistry registry, ItemType type, int nutrition, float modifier,
                             boolean canAlwaysEat, @Nullable ItemType remainder, ConsumableComponent.Effect... effects) {
        food(registry, type, nutrition, modifier, canAlwaysEat, remainder, 32, effects);
    }

    private static void food(CloudItemRegistry registry, ItemType type, int nutrition, float modifier,
                             boolean canAlwaysEat, @Nullable ItemType remainder, int duration, ConsumableComponent.Effect... effects) {
        registry.configure(type)
                .set(ItemBehaviors.FOOD, new FoodComponent(nutrition, nutrition * modifier * 2.0f, canAlwaysEat))
                .set(ItemBehaviors.CONSUMABLE, new ConsumableComponent(remainder, SoundTypes.RANDOM_BURP, List.of(effects)))
                .set(ItemBehaviors.USE_DURATION_TICKS, duration)
                .set(ItemBehaviors.USE, FoodItemHandlers.USE)
                .set(ItemBehaviors.FINISH_USE, FoodItemHandlers.FINISH_USE);
    }
}
