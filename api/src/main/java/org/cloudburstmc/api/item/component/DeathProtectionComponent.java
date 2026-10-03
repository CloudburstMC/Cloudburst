package org.cloudburstmc.api.item.component;

import org.cloudburstmc.api.level.sound.SoundType;
import org.cloudburstmc.api.potion.PotionEffect;

import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * Defines the recovery supplied by a held item that prevents lethal damage.
 *
 * @param health       finite, positive health set on recovery, capped at the entity's maximum
 * @param clearEffects whether removal of existing potion effects is requested before recovery effects
 * @param effects      potion effects applied after recovery, copied into an immutable list
 * @param sound        sound played when protection activates
 */
public record DeathProtectionComponent(float health, boolean clearEffects, List<PotionEffect> effects, SoundType sound) {

    /**
     * Creates a recovery definition with an immutable copy of its effects.
     *
     * @throws IllegalArgumentException if health is not finite and positive
     * @throws NullPointerException     if the effects, an effect entry or the sound is null
     */
    public DeathProtectionComponent {
        if (!Float.isFinite(health) || health <= 0) {
            throw new IllegalArgumentException("Recovery health must be finite and positive");
        }

        effects = List.copyOf(effects);
        requireNonNull(sound, "sound");
    }
}
