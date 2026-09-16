package org.cloudburstmc.api.item.component;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Defines the protection supplied while an item is equipped.
 *
 * @param defense             armor defense points
 * @param toughness           armor toughness
 * @param knockbackResistance fraction of knockback resisted
 */
public record ArmorComponent(int defense, float toughness, float knockbackResistance) {

    public ArmorComponent {
        checkArgument(defense >= 0, "defense must be non-negative");
        checkArgument(Float.isFinite(toughness) && toughness >= 0, "toughness must be finite and non-negative");
        checkArgument(Float.isFinite(knockbackResistance) && knockbackResistance >= 0 && knockbackResistance <= 1, "knockbackResistance must be between zero and one");
    }
}
