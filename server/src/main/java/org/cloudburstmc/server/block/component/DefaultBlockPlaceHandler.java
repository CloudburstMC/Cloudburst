package org.cloudburstmc.server.block.component;

import org.cloudburstmc.api.block.BlockComponents;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockTraits;
import org.cloudburstmc.api.block.component.PlaceBlockHandler;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.util.AxisAlignedBB;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.api.util.component.ComponentMap;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.registry.CloudBlockRegistry;

import java.util.Set;

public class DefaultBlockPlaceHandler implements PlaceBlockHandler {

    private final CloudBlockRegistry registry;

    public DefaultBlockPlaceHandler(CloudBlockRegistry registry) {
        this.registry = registry;
    }

    @Override
    public boolean execute(BlockState blockState, Player player, Vector3i pos, Direction face, Vector3f clickPos) {
        if (player == null) {
            return false;
        }

        Level level = player.getLevel();
        ComponentMap blockComponents = registry.getComponents(blockState.getType());
        AxisAlignedBB boundingBox = blockComponents.get(BlockComponents.GET_BOUNDING_BOX).execute(blockState);

        if (boundingBox != null && hasCollision(boundingBox)) {
            AxisAlignedBB offsetBB = boundingBox.getOffsetBoundingBox(pos.getX(), pos.getY(), pos.getZ());
            Set<? extends Entity> nearbyEntities = level.getCollidingEntities(offsetBB);
            if (!nearbyEntities.isEmpty()) {
                return false;
            }
        }

        if (blockState.getTraits().containsKey(BlockTraits.AXIS)) {
            blockState = blockState.withTrait(BlockTraits.AXIS, face.getAxis());
        }

        return level.setBlockState(pos, blockState, true, true);
    }

    private boolean hasCollision(AxisAlignedBB boundingBox) {
        return boundingBox.getMaxX() > boundingBox.getMinX() ||
                boundingBox.getMaxY() > boundingBox.getMinY() ||
                boundingBox.getMaxZ() > boundingBox.getMinZ();
    }
}
