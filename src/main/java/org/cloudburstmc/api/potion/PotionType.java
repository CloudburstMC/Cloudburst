package org.cloudburstmc.api.potion;

import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import lombok.Setter;

@Data
@Builder
public class PotionType {
    @NonNull
    private final int networkId;
    private final EffectType type;
    private final int level;
    private final int duration;
    private final boolean instant;
    @Setter
    private boolean splash;
}
