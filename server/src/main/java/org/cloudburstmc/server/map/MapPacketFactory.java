package org.cloudburstmc.server.map;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import lombok.experimental.UtilityClass;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.map.MapCursor;
import org.cloudburstmc.api.map.MapView;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.protocol.bedrock.data.MapDecoration;
import org.cloudburstmc.protocol.bedrock.data.MapTrackedObject;
import org.cloudburstmc.protocol.bedrock.packet.ClientboundMapItemDataPacket;
import org.cloudburstmc.server.map.CloudMapView.SentMap;

import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.requireNonNull;

@UtilityClass
public class MapPacketFactory {

    public static @Nullable ClientboundMapItemDataPacket create(
            MapView map, int dimension, long[] mapIds, int[] pixels, List<MapCursor> cursors,
            @Nullable SentMap previous
    ) {
        int minX = 128;
        int minY = 128;
        int maxX = -1;
        int maxY = -1;

        for (int i = 0; i < pixels.length; i++) {
            if (previous == null || pixels[i] != previous.pixels()[i]) {
                minX = Math.min(minX, i % 128);
                minY = Math.min(minY, i / 128);
                maxX = Math.max(maxX, i % 128);
                maxY = Math.max(maxY, i / 128);
            }
        }

        boolean cursorsChanged = previous == null || !cursors.equals(previous.cursors());
        if (maxX < 0 && !cursorsChanged) {
            return null;
        }

        ClientboundMapItemDataPacket packet = new ClientboundMapItemDataPacket();
        packet.setUniqueMapId(map.getId());
        packet.setDimensionId(dimension);
        packet.setLocked(map.isLocked());
        packet.setOrigin(Vector3i.from(map.getCenterX(), 0, map.getCenterZ()));
        packet.setScale((byte) Integer.numberOfTrailingZeros(map.getScale().getBlocksPerPixel()));

        if (previous == null) {
            packet.setTrackedEntityIds(new LongArrayList(mapIds));
        }

        if (cursorsChanged) {
            List<MapDecoration> decorations = new ArrayList<>();
            List<MapTrackedObject> objects = new ArrayList<>();

            for (MapCursor cursor : cursors) {
                int image = switch (cursor.type()) {
                    case PLAYER -> 0;
                    case FRAME -> 7;
                    case PLAYER_OFF_MAP -> 6;
                    case PLAYER_OFF_LIMITS -> 13;
                };

                decorations.add(new MapDecoration(image, cursor.rotation(), cursor.x(), cursor.y(), cursor.caption(), cursor.color().getRGB()));
                Entity entity = cursor.trackedEntity();
                objects.add(new MapTrackedObject(entity == null ? Long.MIN_VALUE + objects.size() : entity.getUniqueId()));
            }

            packet.setDecorations(decorations);
            packet.setTrackedObjects(objects);
        }

        if (maxX >= 0) {
            int width = maxX - minX + 1;
            int height = maxY - minY + 1;
            int[] colors = new int[width * height];

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    colors[y * width + x] = toAbgr(pixels[(y + minY) * 128 + x + minX]);
                }
            }

            packet.setXOffset(minX);
            packet.setYOffset(minY);
            packet.setWidth(width);
            packet.setHeight(height);
            packet.setColors(colors);
        }

        return packet;
    }

    public static ClientboundMapItemDataPacket createLockedCopy(MapView source, int dimension, long[] mapIds, int[] pixels, long previewId) {
        ClientboundMapItemDataPacket packet = requireNonNull(create(source, dimension, mapIds, pixels, List.of(), null));
        long[] hierarchy = mapIds.clone();
        hierarchy[0] = previewId;
        packet.setUniqueMapId(previewId);
        packet.setTrackedEntityIds(new LongArrayList(hierarchy));
        packet.setLocked(true);
        return packet;
    }

    public static int toAbgr(int argb) {
        return (argb & 0xff00ff00) | ((argb & 0xff0000) >>> 16) | ((argb & 0xff) << 16);
    }
}
