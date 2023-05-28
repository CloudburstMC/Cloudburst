package org.cloudburstmc.api.player.skin.data;

import lombok.Data;

@Data
public class PersonaPiece {
    private final String id;
    private final String type;
    private final String packId;
    private final boolean isDefault;
    private final String productId;
}
