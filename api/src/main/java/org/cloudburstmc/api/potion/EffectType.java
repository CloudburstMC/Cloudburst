package org.cloudburstmc.api.potion;

import lombok.Getter;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.math.vector.Vector3i;

@Getter
public class EffectType {
    private final Identifier id;
    private final Vector3i color;
    private final boolean bad;

    EffectType(Identifier id, Vector3i color) {
        this(id, color, false);
    }

    EffectType(Identifier id, Vector3i color, boolean isBad) {
        this.id = id;
        this.color = color;
        this.bad = isBad;
    }
}
