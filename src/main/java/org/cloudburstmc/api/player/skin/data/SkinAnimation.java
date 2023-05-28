package org.cloudburstmc.api.player.skin.data;

import lombok.Data;

@Data
public class SkinAnimation {
    private final ImageData image;
    private final int type;
    private final float frames;
    private final int expression;
}
