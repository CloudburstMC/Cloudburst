package org.cloudburstmc.api.potion;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@RequiredArgsConstructor
@Builder
public class PotionType {
    private final int networkId;
    private final EffectType type;
    private final int level;
    private final int duration;
    private final boolean instant;
    @Setter
    private boolean splash;
}
