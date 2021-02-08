package org.cloudburstmc.api.potion;

import lombok.*;

@Getter
@RequiredArgsConstructor
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
