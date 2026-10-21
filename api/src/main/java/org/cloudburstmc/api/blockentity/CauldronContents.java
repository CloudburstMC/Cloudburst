package org.cloudburstmc.api.blockentity;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.potion.PotionForm;
import org.cloudburstmc.api.potion.PotionType;

import java.util.Objects;

/**
 * Potion or dye stored in a cauldron, independent of its liquid type and fill level.
 *
 * @param potion stored potion, or null for ordinary or dyed liquid
 * @param form   potion form, which must be drinkable when no potion is stored
 * @param color  dye as a 24-bit RGB value, or null when undyed
 */
public record CauldronContents(@Nullable PotionType potion, PotionForm form, @Nullable Integer color) {

    /**
     * Ordinary liquid without a potion or dye.
     */
    public static final CauldronContents PLAIN = new CauldronContents(null, PotionForm.DRINKABLE, null);

    /**
     * @throws IllegalArgumentException if the color is outside 0x000000 to 0xFFFFFF,
     *                                  a potion is combined with dye, or a form is supplied without a potion
     */
    public CauldronContents {
        Objects.requireNonNull(form, "form");

        if (color != null && (color & 0xFF000000) != 0) {
            throw new IllegalArgumentException("Color must be a 24-bit RGB value");
        }

        if (potion != null && color != null) {
            throw new IllegalArgumentException("Potion contents cannot also contain dye");
        }

        if (potion == null && form != PotionForm.DRINKABLE) {
            throw new IllegalArgumentException("A potion form requires a potion");
        }
    }
}
