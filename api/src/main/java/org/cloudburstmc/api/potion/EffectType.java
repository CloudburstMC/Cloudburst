package org.cloudburstmc.api.potion;

import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.math.vector.Vector3i;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Identifies an entity effect and its display properties.
 */
public class EffectType {
    private final Identifier id;
    private final Vector3i color;
    private final EffectCategory category;

    private EffectType(Identifier id, Vector3i color, EffectCategory category) {
        this.id = checkNotNull(id, "id");
        this.color = checkNotNull(color, "color");
        this.category = checkNotNull(category, "category");
    }

    /**
     * Creates an effect type.
     *
     * @param id       effect identifier
     * @param color    display color
     * @param category effect category
     * @return effect type
     */
    public static EffectType of(Identifier id, Vector3i color, EffectCategory category) {
        return new EffectType(id, color, category);
    }

    /**
     * Returns the effect identifier.
     *
     * @return effect identifier
     */
    public Identifier getId() {
        return this.id;
    }

    /**
     * Returns the color used to represent this effect.
     *
     * @return effect color
     */
    public Vector3i getColor() {
        return this.color;
    }

    /**
     * Returns the effect category.
     *
     * @return effect category
     */
    public EffectCategory getCategory() {
        return this.category;
    }

    @Override
    public String toString() {
        return "EffectType{id=" + this.id + '}';
    }
}
