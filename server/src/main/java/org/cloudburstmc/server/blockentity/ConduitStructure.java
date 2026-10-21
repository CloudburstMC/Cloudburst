package org.cloudburstmc.server.blockentity;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.BlockTags;
import org.cloudburstmc.api.block.LiquidTypes;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.math.vector.Vector3i;

import java.util.ArrayList;
import java.util.List;

public record ConduitStructure(List<Vector3i> frameBlocks) {

    private static final ConduitStructure EMPTY = new ConduitStructure(List.of());
    private static final List<Vector3i> FRAME_OFFSETS = createFrameOffsets();

    public ConduitStructure {
        frameBlocks = List.copyOf(frameBlocks);
    }

    public static ConduitStructure scan(Level level, Vector3i origin) {
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    Block block = level.getLoadedBlock(origin.add(x, y, z));
                    if (block == null || !block.getLiquid().getType().isSameFamily(LiquidTypes.WATER)) {
                        return EMPTY;
                    }
                }
            }
        }

        List<Vector3i> frame = new ArrayList<>(FRAME_OFFSETS.size());
        for (Vector3i offset : FRAME_OFFSETS) {
            Vector3i position = origin.add(offset);
            Block block = level.getLoadedBlock(position);
            if (block == null) {
                return EMPTY;
            }

            if (block.getState().is(BlockTags.CONDUIT_EFFECT_BLOCK)) {
                frame.add(position);
            }
        }

        return new ConduitStructure(frame);
    }

    public boolean isActive() {
        return this.frameBlocks.size() >= 16;
    }

    public boolean isHunting() {
        return this.frameBlocks.size() >= 42;
    }

    public int range() {
        return this.isActive() ? this.frameBlocks.size() / 7 * 16 : 0;
    }

    private static List<Vector3i> createFrameOffsets() {
        List<Vector3i> offsets = new ArrayList<>(42);
        for (int x = -2; x <= 2; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -2; z <= 2; z++) {
                    int ax = Math.abs(x);
                    int ay = Math.abs(y);
                    int az = Math.abs(z);
                    if (x == 0 && (ay == 2 || az == 2) || y == 0 && (ax == 2 || az == 2) || z == 0 && (ax == 2 || ay == 2)) {
                        offsets.add(Vector3i.from(x, y, z));
                    }
                }
            }
        }

        return List.copyOf(offsets);
    }
}
