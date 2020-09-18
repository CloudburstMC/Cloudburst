package org.cloudburstmc.server.item.data;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import javax.annotation.concurrent.Immutable;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@Immutable
public class MapItem {

    private final long id;
    private final long parentId;
    private final byte[] colors;

    public static MapItem of(long id, long parentId, byte[] colors) {
        return new MapItem(id, parentId, colors);
    }

    public static MapItem fromImage(long id, long parentId, BufferedImage img) throws IOException {
        BufferedImage image = img;

        if (img.getHeight() != 128 || img.getWidth() != 128) { //resize
            image = new BufferedImage(128, 128, img.getType());
            Graphics2D g = image.createGraphics();
            g.drawImage(img, 0, 0, 128, 128, null);
            g.dispose();
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);

        return of(id, parentId, baos.toByteArray());
    }
}
