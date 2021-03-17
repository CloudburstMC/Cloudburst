package org.cloudburstmc.server.item.food;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import lombok.val;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.event.player.PlayerEatFoodEvent;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemType;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.potion.EffectTypes;
import org.cloudburstmc.server.item.data.Bucket;
import org.cloudburstmc.server.player.CloudPlayer;
import org.cloudburstmc.server.potion.CloudEffect;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.*;

import static org.cloudburstmc.server.item.ItemTypes.*;

/**
 * Created by Snake1999 on 2016/1/13.
 * Package cn.nukkit.item.food in project nukkit.
 */
@ParametersAreNonnullByDefault
public abstract class Food implements org.cloudburstmc.api.item.Food {

    private static final Map<ItemType, Food> registryDefault = new LinkedHashMap<>();

    public static final Food apple = registerFood(APPLE, new FoodNormal(4, 2.4F));
    public static final Food apple_golden = registerFood(GOLDEN_APPLE, new FoodEffective(4, 9.6F)
            .addEffect(new CloudEffect(EffectTypes.REGENERATION).setAmplifier(1).setDuration(5 * 20))
            .addEffect(new CloudEffect(EffectTypes.ABSORPTION).setDuration(2 * 60 * 20)));
    public static final Food apple_golden_enchanted = registerFood(APPLE_ENCHANTED, new FoodEffective(4, 9.6F)
            .addEffect(new CloudEffect(EffectTypes.REGENERATION).setAmplifier(4).setDuration(30 * 20))
            .addEffect(new CloudEffect(EffectTypes.ABSORPTION).setDuration(2 * 60 * 20).setAmplifier(3))
            .addEffect(new CloudEffect(EffectTypes.DAMAGE_RESISTANCE).setDuration(5 * 60 * 20))
            .addEffect(new CloudEffect(EffectTypes.FIRE_RESISTANCE).setDuration(5 * 60 * 20)));
    public static final Food beef_raw = registerFood(BEEF, new FoodNormal(3, 1.8F));
    public static final Food beetroot = registerFood(BEETROOT, new FoodNormal(1, 1.2F));
    public static final Food beetroot_soup = registerFood(BEETROOT_SOUP, new FoodInBowl(6, 7.2F));
    public static final Food bread = registerFood(BREAD, new FoodNormal(5, 6F));
    public static final Food cake_slice = registerFood(BlockTypes.CAKE, new FoodNormal(2, 0.4F));
    public static final Food carrot = registerFood(CARROT, new FoodNormal(3, 4.8F));
    public static final Food carrot_golden = registerFood(GOLDEN_CARROT, new FoodNormal(6, 14.4F));
    public static final Food chicken_raw = registerFood(CHICKEN, new FoodEffective(2, 1.2F)
            .addChanceEffect(0.3F, new CloudEffect(EffectTypes.HUNGER).setDuration(30 * 20)));
    public static final Food chicken_cooked = registerFood(COOKED_CHICKEN, new FoodNormal(6, 7.2F));
    public static final Food chorus_fruit = registerFood(CHORUS_FRUIT, new FoodChorusFruit());
    public static final Food cookie = registerFood(COOKIE, new FoodNormal(2, 0.4F));
    public static final Food melon_slice = registerFood(MELON, new FoodNormal(2, 1.2F));
    public static final Food milk = registerFood(BUCKET, new FoodMilk().setMetadata(Bucket.MILK));
    public static final Food mushroom_stew = registerFood(MUSHROOM_STEW, new FoodInBowl(6, 7.2F));
    public static final Food mutton_cooked = registerFood(MUTTON_COOKED, new FoodNormal(6, 9.6F));
    public static final Food mutton_raw = registerFood(MUTTON_RAW, new FoodNormal(2, 1.2F));
    public static final Food porkchop_cooked = registerFood(COOKED_PORKCHOP, new FoodNormal(8, 12.8F));
    public static final Food porkchop_raw = registerFood(PORKCHOP, new FoodNormal(3, 1.8F));
    public static final Food potato_raw = registerFood(POTATO, new FoodNormal(1, 0.6F));
    public static final Food potato_baked = registerFood(BAKED_POTATO, new FoodNormal(5, 7.2F));
    public static final Food potato_poisonous = registerFood(POISONOUS_POTATO, new FoodEffective(2, 1.2F)
            .addChanceEffect(0.6F, new CloudEffect(EffectTypes.POISON).setDuration(4 * 20)));
    public static final Food pumpkin_pie = registerFood(PUMPKIN_PIE, new FoodNormal(8, 4.8F));
    public static final Food rabbit_cooked = registerFood(COOKED_RABBIT, new FoodNormal(5, 6F));
    public static final Food rabbit_raw = registerFood(RABBIT, new FoodNormal(3, 1.8F));
    public static final Food rabbit_stew = registerFood(RABBIT_STEW, new FoodInBowl(10, 12F));
    public static final Food rotten_flesh = registerFood(ROTTEN_FLESH, new FoodEffective(4, 0.8F)
            .addChanceEffect(0.8F, new CloudEffect(EffectTypes.HUNGER).setDuration(30 * 20)));
    public static final Food spider_eye = registerFood(SPIDER_EYE, new FoodEffective(2, 3.2F)
            .addEffect(new CloudEffect(EffectTypes.POISON).setDuration(4 * 20)));
    public static final Food steak = registerFood(COOKED_BEEF, new FoodNormal(8, 12.8F));
    //different kinds of fishes
    public static final Food clownfish = registerFood(CLOWNFISH, new FoodNormal(1, 0.2F));
    public static final Food fish_cooked = registerFood(COOKED_FISH, new FoodNormal(5, 6F));
    public static final Food fish_raw = registerFood(FISH, new FoodNormal(2, 0.4F));
    public static final Food salmon_cooked = registerFood(COOKED_SALMON, new FoodNormal(6, 9.6F));
    public static final Food salmon_raw = registerFood(SALMON, new FoodNormal(2, 0.4F));
    public static final Food pufferfish = registerFood(PUFFERFISH, new FoodEffective(1, 0.2F)
            .addEffect(new CloudEffect(EffectTypes.HUNGER).setAmplifier(2).setDuration(15 * 20))
            .addEffect(new CloudEffect(EffectTypes.NAUSEA).setAmplifier(1).setDuration(15 * 20))
            .addEffect(new CloudEffect(EffectTypes.POISON).setAmplifier(4).setDuration(60 * 20)));
    public static final Food dried_kelp = registerFood(DRIED_KELP, new FoodNormal(1, 0.6F));
    public static final Food sweet_berries = registerFood(SWEET_BERRIES, new FoodNormal(2, 0.4F));
    public static final Food honey = registerFood(HONEY_BOTTLE, new FoodHoney(6, 2.4F));

    private static Food registerFood(ItemType type, Food food) {
        registryDefault.put(type, food);
        return food;
    }

    public static Food getByRelative(ItemStack item) {
        val food = registryDefault.get(item.getType());

        if (food.metadata.isEmpty()) {
            return food;
        }

        setlooop:
        for (Set<Object> data : food.metadata) {
            if (data.isEmpty()) {
                return food;
            }

            for (Object value : data) {
                if (item.getMetadata(value.getClass()) != value) {
                    continue setlooop;
                }
            }
        }

        return null;
    }

    protected int restoreFood = 0;
    protected float restoreSaturation = 0;
    protected final List<Set<Object>> metadata = new ArrayList<>();

    public final boolean eatenBy(Player player) {
        PlayerEatFoodEvent event = new PlayerEatFoodEvent(player, this);
        player.getServer().getEventManager().fire(event);
        if (event.isCancelled()) return false;
        return event.getFood().onEatenBy(player);
    }

    public boolean onEatenBy(Player player) {
        Preconditions.checkNotNull(player, "player");
        ((CloudPlayer) player).getFoodData().addFoodLevel(this);
        return true;
    }

    public Food setMetadata(Object... data) {
        Preconditions.checkNotNull(data, "data");
        if (data.length > 0) {
            metadata.add(ImmutableSet.copyOf(data));
        }
        return this;
    }

    public int getRestoreFood() {
        return restoreFood;
    }

    public Food setRestoreFood(int restoreFood) {
        this.restoreFood = restoreFood;
        return this;
    }

    public float getRestoreSaturation() {
        return restoreSaturation;
    }

    public Food setRestoreSaturation(float restoreSaturation) {
        this.restoreSaturation = restoreSaturation;
        return this;
    }
}
