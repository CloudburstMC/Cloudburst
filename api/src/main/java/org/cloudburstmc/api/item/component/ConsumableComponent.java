package org.cloudburstmc.api.item.component;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.item.ItemType;
import org.cloudburstmc.api.level.sound.SoundType;
import org.cloudburstmc.api.potion.PotionEffect;

import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * The result and feedback of consuming an item after its use duration completes.
 *
 * @param remainder item returned after consuming one item, or {@code null}
 * @param sound     sound played when consumption completes
 * @param effects   potion effects applied after consumption
 */
public record ConsumableComponent(@Nullable ItemType remainder, SoundType sound, List<Effect> effects) {

    public ConsumableComponent {
        requireNonNull(sound, "sound");
        effects = List.copyOf(effects);
    }

    /**
     * @param effect      effect to apply
     * @param probability chance between zero and one, inclusive
     */
    public record Effect(PotionEffect effect, float probability) {
        public Effect {
            requireNonNull(effect, "effect");
            if (!Float.isFinite(probability) || probability < 0.0f || probability > 1.0f) {
                throw new IllegalArgumentException("Probability must be between zero and one");
            }
        }
    }
}
