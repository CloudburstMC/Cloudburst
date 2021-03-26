package org.cloudburstmc.api.potion;

import com.nukkitx.math.vector.Vector3i;
import lombok.Getter;
import org.cloudburstmc.api.util.Identifier;

import javax.annotation.Nullable;
import java.util.HashMap;

@Getter
public class EffectType {
    private final Identifier id;
    private final Vector3i color;
    private final boolean bad;
    private static final HashMap<Identifier, EffectType> effectMap = new HashMap<>();

    EffectType(Identifier id, Vector3i color) {
        this(id, color, false);
    }

    EffectType(Identifier id, Vector3i color, boolean isBad) {
        this.id = id;
        this.color = color;
        this.bad = isBad;

        effectMap.put(id, this);
    }

    @Nullable
    public static EffectType byName(String arg) {
        return effectMap.get(Identifier.fromString(arg));
    }
}
