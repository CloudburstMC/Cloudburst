package org.cloudburstmc.server.item.food;

import org.cloudburstmc.api.event.entity.PotionEffectCause;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.potion.PotionEffect;
import org.cloudburstmc.server.player.CloudPlayer;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

public class FoodEffective extends Food {

    protected final Map<PotionEffect, Float> effects = new LinkedHashMap<>();

    public FoodEffective(int restoreFood, float restoreSaturation) {
        this.setRestoreFood(restoreFood);
        this.setRestoreSaturation(restoreSaturation);
    }

    public FoodEffective addEffect(PotionEffect effect) {
        return this.addChanceEffect(1F, effect);
    }

    public FoodEffective addChanceEffect(float chance, PotionEffect effect) {
        checkArgument(Float.isFinite(chance) && chance >= 0 && chance <= 1,
                "chance must be between 0 and 1");
        this.effects.put(requireNonNull(effect, "effect"), chance);
        return this;
    }

    @Override
    public boolean onEatenBy(Player player) {
        super.onEatenBy(player);
        CloudPlayer cloudPlayer = (CloudPlayer) player;
        this.effects.forEach((effect, chance) -> {
            if (ThreadLocalRandom.current().nextFloat() <= chance) {
                cloudPlayer.addPotionEffect(effect, null, PotionEffectCause.FOOD);
            }
        });

        return true;
    }
}
