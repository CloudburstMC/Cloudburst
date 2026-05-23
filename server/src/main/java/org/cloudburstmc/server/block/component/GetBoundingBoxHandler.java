package org.cloudburstmc.server.block.component;

import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.component.AABBBlockHandler;
import org.cloudburstmc.api.util.AxisAlignedBB;
import org.cloudburstmc.api.util.SimpleAxisAlignedBB;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;

public class GetBoundingBoxHandler implements AABBBlockHandler {

    private static final AxisAlignedBB DEFAULT_BOUNDING_BOX = new SimpleAxisAlignedBB(Vector3i.ZERO, Vector3i.ONE);

    @Override
    public AxisAlignedBB execute(BlockState state) {
        float[] boxes = state.getCollisionBoxes();
        if (boxes != null && boxes.length >= 6) {
            return new SimpleAxisAlignedBB(
                    Vector3f.from(boxes[0], boxes[1], boxes[2]),
                    Vector3f.from(boxes[3], boxes[4], boxes[5])
            );
        }
        return DEFAULT_BOUNDING_BOX.clone();
    }
}
