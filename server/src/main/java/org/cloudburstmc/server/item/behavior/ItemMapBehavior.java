package org.cloudburstmc.server.item.behavior;

import lombok.extern.log4j.Log4j2;
import org.cloudburstmc.server.item.ItemStack;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by CreeperFace on 18.3.2017.
 */
@Log4j2
public class ItemMapBehavior extends CloudItemBehavior {

    public static final AtomicLong mapIdAllocator = new AtomicLong(0);

    private long mapId;
    private long parentMapId;
    private byte dimension;
    private int xCenter;
    private int zCenter;
    private int scale;
    private boolean unlimitedTracking;
    private boolean previewIncomplete;
    private short width;
    private short height;
    private int[] image = new int[0];
    private boolean fullyExplored;
    // private List<MapDecoration> decorations
    private boolean mapLocked;

    public ItemMapBehavior() {
        this.mapId = mapIdAllocator.getAndIncrement();
    }

//    public void setImage(File file) throws IOException {
//        setImage(ImageIO.read(file));
//    }
//
//    protected BufferedImage getBufferedImage() {
//        try {
//            byte[] data = getColors();
//            return ImageIO.read(new ByteArrayInputStream(data));
//        } catch (IOException e) {
//            log.error("Unable to load image from NBT", e);
//        }
//
//        return null;
//    }

//    public void setImage(int[] image) {
//        this.image = Arrays.copyOf(image, image.length);
//    }
//
//    public byte[] getColors() {
//        byte[] image = new byte[this.image.length * 4];
//        for (int i = 0; i < this.image.length; i += 4) {
//            int rgba = this.image[i];
//            image[i] = (byte) ((rgba >>> 24) & 0xff);
//            image[i + 1] = (byte) ((rgba >>> 16) & 0xff);
//            image[i + 2] = (byte) ((rgba >>> 8) & 0xff);
//            image[i + 3] = (byte) (rgba & 0xff);
//        }
//        return image;
//    }
//
//    public void setColors(byte[] colors) {
//        checkNotNull(colors, "colors");
//        int[] image = new int[colors.length / 4];
//        for (int i = 0; i < image.length; i += 4) {
//            image[i] = (colors[i] << 24) | (colors[i + 1] << 16) | (colors[i + 2] << 8) | colors[i + 3];
//        }
//        this.image = image;
//    }
//
//    public boolean isFullyExplored() {
//        return fullyExplored;
//    }
//
//    public void setFullyExplored(boolean fullyExplored) {
//        this.fullyExplored = fullyExplored;
//    }
//
//    public boolean isMapLocked() {
//        return mapLocked;
//    }
//
//    public void setMapLocked(boolean mapLocked) {
//        this.mapLocked = mapLocked;
//    }
//
//    public void sendImage(Player p) {
//        ClientboundMapItemDataPacket packet = new ClientboundMapItemDataPacket();
//        packet.setUniqueMapId(this.mapId);
//        packet.setDimensionId(this.dimension);
//        packet.setLocked(this.mapLocked);
//        packet.setXOffset(this.xCenter);
//        packet.setYOffset(this.zCenter);
//        packet.setWidth(this.width);
//        packet.setHeight(this.height);
//        packet.setScale(this.scale);
//        packet.setColors(this.image);
//
//        p.sendPacket(packet);
//    }

    @Override
    public boolean canBeActivated() {
        return true;
    }

    @Override
    public int getMaxStackSize(ItemStack item) {
        return 1;
    }
}
