package org.cloudburstmc.api.potion;

import org.cloudburstmc.api.util.Identifier;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * An immutable potion definition and its base effects.
 */
public class PotionType {
    private final Identifier id;
    private final List<PotionEffect> effects;

    private PotionType(Identifier id, List<PotionEffect> effects) {
        this.id = checkNotNull(id, "id");
        this.effects = List.copyOf(checkNotNull(effects, "effects"));
    }

    /**
     * Creates a potion type.
     *
     * @param id      potion identifier
     * @param effects base effects applied by the potion
     * @return potion type
     */
    public static PotionType of(Identifier id, PotionEffect... effects) {
        checkNotNull(effects, "effects");
        return new PotionType(id, List.of(effects));
    }

    /**
     * Returns the potion identifier.
     *
     * @return potion identifier
     */
    public Identifier getId() {
        return this.id;
    }

    /**
     * Returns the potion's base effects.
     *
     * @return immutable effect list
     */
    public List<PotionEffect> getEffects() {
        return this.effects;
    }

    @Override
    public String toString() {
        return "PotionType{id=" + this.id + '}';
    }
}
