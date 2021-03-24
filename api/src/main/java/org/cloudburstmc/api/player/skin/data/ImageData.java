package org.cloudburstmc.api.player.skin.data;

import lombok.Data;

@Data
public class ImageData {
    public static final ImageData EMPTY = new ImageData(0, 0, new byte[0]);

    private final int width;
    private final int height;
    private final byte[] image;
}

