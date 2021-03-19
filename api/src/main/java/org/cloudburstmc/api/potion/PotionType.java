package org.cloudburstmc.api.potion;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
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

    private static final BiMap<Identifier, Byte> netIdMap = HashBiMap.create();
    private static final HashMap<Identifier, PotionType> typeMap = new HashMap<>();

    @Nonnull
    private Identifier potionId;
    private final int networkId;
    private final EffectType type;
    private final int level;
    private final int duration;
    private final boolean instant;
    @Setter
    private boolean splash;

    public PotionType(Identifier id, int netId, EffectType type, int level, int duration, boolean instant) {
        this(id, netId, type, level, duration, instant, false);
    }

    public PotionType(Identifier id, int netId, EffectType type, int level, int duration, boolean instant, boolean splash) {
        this.networkId= netId;
        this.type = type;
        this.level = level;
        this.duration = duration;
        this.instant = instant;
        this.splash = splash;

        netIdMap.put(id, (byte) netId);
        typeMap.put(id, this);
    }

    @Nullable
    public static PotionType fromLegacy(byte networkId) {
        return typeMap.get(netIdMap.inverse().get(networkId));
    }

    @Nullable
    public static PotionType byName(String name) {
        return typeMap.get(Identifier.fromString(name));
    }

}
