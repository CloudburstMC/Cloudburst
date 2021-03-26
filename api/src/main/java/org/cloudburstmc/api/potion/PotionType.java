package org.cloudburstmc.api.potion;

import lombok.Builder;
import lombok.Data;
import lombok.Setter;
import org.cloudburstmc.api.util.Identifier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;

@Data
@Builder
public class PotionType {

    private static final HashMap<Identifier, PotionType> typeMap = new HashMap<>();

    @Nonnull
    private Identifier potionId;
    private final EffectType type;
    private final int level;
    private final int duration;
    private final boolean instant;
    @Setter
    private boolean splash;

    public PotionType(Identifier id, EffectType type, int level, int duration, boolean instant) {
        this(id, type, level, duration, instant, false);
    }

    public PotionType(Identifier id, EffectType type, int level, int duration, boolean instant, boolean splash) {
        this.type = type;
        this.level = level;
        this.duration = duration;
        this.instant = instant;
        this.splash = splash;

        typeMap.put(id, this);
    }

    @Nullable
    public static PotionType byName(String name) {
        return typeMap.get(Identifier.fromString(name));
    }

}
