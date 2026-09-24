package org.cloudburstmc.server.potion;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.potion.PotionEffect;
import org.cloudburstmc.math.vector.Vector3i;

import java.util.Collection;
import java.util.OptionalInt;

@UtilityClass
public class CloudPotionColor {
    public static OptionalInt calculateEffects(Collection<PotionEffect> effects) {
        int red = 0;
        int green = 0;
        int blue = 0;
        int totalWeight = 0;

        for (PotionEffect effect : effects) {
            Vector3i color = effect.getType().getColor();
            int weight = effect.getAmplifier() + 1;
            red += color.getX() * weight;
            green += color.getY() * weight;
            blue += color.getZ() * weight;
            totalWeight += weight;
        }

        if (totalWeight == 0) {
            return OptionalInt.empty();
        }

        return OptionalInt.of((red / totalWeight) << 16 | (green / totalWeight) << 8 | blue / totalWeight);
    }
}
