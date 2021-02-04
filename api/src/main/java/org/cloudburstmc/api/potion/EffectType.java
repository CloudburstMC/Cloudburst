package org.cloudburstmc.api.potion;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.math.vector.Vector3i;

@RequiredArgsConstructor
@Getter
@Builder
public class EffectType {
    private final byte id;
    private final Identifier type;
    private final byte amplifier;
    private final int duration;
    private final Vector3i color;
    private final boolean bad;
}
