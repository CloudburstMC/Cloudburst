package org.cloudburstmc.server.level.light;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.registry.BlockRegistry;
import org.cloudburstmc.math.vector.Vector3i;

public interface LightAccess {

    BlockRegistry registry();

    int minHeight();

    int maxHeight();

    @Nullable
    Sample read(Vector3i position);

    void write(Vector3i position, int light);

    enum Type {
        BLOCK,
        SKY
    }

    record Sample(BlockState primary, BlockState secondary, int light) {

        public int opacity() {
            return Math.max(this.primary.getLightDampening(), this.secondary.getLightDampening());
        }

        public int emission() {
            return Math.max(this.primary.getLightEmission(), this.secondary.getLightEmission());
        }
    }
}
