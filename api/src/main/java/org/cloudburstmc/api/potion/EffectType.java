package org.cloudburstmc.api.potion;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.nukkitx.math.vector.Vector3i;
import lombok.Getter;
import org.cloudburstmc.api.util.Identifier;

import javax.annotation.Nullable;
import java.util.HashMap;

@Getter
public class EffectType {
    private final byte networkId;
    private final Identifier id;
    private final Vector3i color;
    private final boolean bad;
    private static final BiMap<Identifier, Byte> netIdMap = HashBiMap.create();
    private static final HashMap<Identifier, EffectType> effectMap = new HashMap<>();

    EffectType(Identifier id, byte networkId, Vector3i color) {
        this(id, networkId, color, false);
    }

    EffectType(Identifier id, byte networkId, Vector3i color, boolean isBad) {
        this.id = id;
        this.networkId = networkId;
        this.color = color;
        this.bad = isBad;

        netIdMap.put(id, networkId);
        effectMap.put(id, this);
    }

    public static EffectType fromLegacy(byte id) {
        return effectMap.get(netIdMap.inverse().get(id));
    }

    @Nullable
    public static EffectType byName(String arg) {
        Identifier id = Identifier.fromString(arg);

        for(EffectType type : effectMap.values()) {
            if(type.getId().equals(id))
                return type;
        }
        return null;
    }
}
