package org.cloudburstmc.server.level.collision;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.util.BoundingBox;
import org.cloudburstmc.math.GenericMath;

@UtilityClass
public class BlockBoxTraversal {

    public static void forEach(BoundingBox box, BlockPositionConsumer consumer) {
        int minX = GenericMath.floor(box.getMinX());
        int minY = GenericMath.floor(box.getMinY());
        int minZ = GenericMath.floor(box.getMinZ());
        int maxX = GenericMath.floor(box.getMaxX());
        int maxY = GenericMath.floor(box.getMaxY());
        int maxZ = GenericMath.floor(box.getMaxZ());

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    consumer.accept(x, y, z);
                }
            }
        }
    }

    @FunctionalInterface
    public interface BlockPositionConsumer {
        void accept(int x, int y, int z);
    }
}
