package org.cloudburstmc.server.level.chunk;

import org.cloudburstmc.api.block.BlockLayer;
import org.cloudburstmc.server.level.light.LightPositions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CloudChunkLightPositionTest {

    @Test
    void lightPositionMatchesBlockKeyWithLayer() {
        int[] minHeights = {-64, 0};
        int[][] origins = {{0, 0}, {-3, 5}};
        BlockLayer[] layers = {BlockLayer.PRIMARY, BlockLayer.SECONDARY};

        for (int minHeight : minHeights) {
            int[] heights = {minHeight, minHeight + 1, minHeight + 16, minHeight + 511};

            for (int[] origin : origins) {
                for (BlockLayer layer : layers) {
                    for (int localX = 0; localX < 16; localX++) {
                        for (int localZ = 0; localZ < 16; localZ++) {
                            for (int y : heights) {
                                int x = (origin[0] << 4) + localX;
                                int z = (origin[1] << 4) + localZ;
                                int blockKey = CloudChunk.blockKeyWithLayer(x, y, z, layer, minHeight);
                                long packed = CloudChunk.lightPosition(CloudChunk.key(origin[0], origin[1]), blockKey, minHeight);
                                assertEquals(x, LightPositions.x(packed), () -> "x of " + x + "," + y + "," + z);
                                assertEquals(y, LightPositions.y(packed), () -> "y of " + x + "," + y + "," + z);
                                assertEquals(z, LightPositions.z(packed), () -> "z of " + x + "," + y + "," + z);
                            }
                        }
                    }
                }
            }
        }
    }
}
