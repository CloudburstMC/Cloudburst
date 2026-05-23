package org.cloudburstmc.server.block.component;

import org.cloudburstmc.api.block.BlockComponents;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockTraits;
import org.cloudburstmc.api.block.component.PlaceBlockHandler;
import org.cloudburstmc.api.block.trait.BlockTrait;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.util.AxisAlignedBB;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.api.util.component.ComponentMap;
import org.cloudburstmc.api.util.data.CardinalDirection;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.registry.CloudBlockRegistry;

import java.util.Map;
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

        blockState = applyDirectionTraits(blockState, player, pos, face);
        return level.setBlockState(pos, blockState, true, true);
    }

    protected BlockState applyDirectionTraits(BlockState blockState, Player player, Vector3i pos, Direction face) {
        Map<BlockTrait<?>, Comparable<?>> traits = blockState.getTraits();
        if (traits.containsKey(BlockTraits.AXIS)) {
            blockState = blockState.withTrait(BlockTraits.AXIS, face.getAxis());
        }

        if (traits.containsKey(BlockTraits.CARDINAL_DIRECTION)) {
            Direction horizontal = player.getHorizontalDirection().getOpposite();
            blockState = blockState.withTrait(BlockTraits.CARDINAL_DIRECTION, horizontal.getCardinalDirection());
        }

        if (traits.containsKey(BlockTraits.DIRECTION)) {
            Direction horizontal = player.getHorizontalDirection().getOpposite();
            blockState = blockState.withTrait(BlockTraits.DIRECTION, horizontal);
        }

        if (traits.containsKey(BlockTraits.FACING_DIRECTION)) {
            blockState = blockState.withTrait(BlockTraits.FACING_DIRECTION, resolveFacingDirection(player));
        }

        if (traits.containsKey(BlockTraits.SIGN_DIRECTION)) {
            blockState = blockState.withTrait(BlockTraits.SIGN_DIRECTION, yawToCardinalDirection(player.getYaw()));
        }

        return blockState;
    }

    protected Direction resolveFacingDirection(Player player) {
        float pitch = player.getPitch();
        if (pitch < -45f) {
            return Direction.UP;
        }

        if (pitch > 45f) {
            return Direction.DOWN;
        }

        return player.getHorizontalDirection().getOpposite();
    }

    protected CardinalDirection yawToCardinalDirection(float yaw) {
        int index = Math.round(((yaw % 360f) + 360f) % 360f / 22.5f) % 16;
        return CardinalDirection.values()[index];
    }

    private boolean hasCollision(AxisAlignedBB boundingBox) {
        return boundingBox.getMaxX() > boundingBox.getMinX() ||
                boundingBox.getMaxY() > boundingBox.getMinY() ||
                boundingBox.getMaxZ() > boundingBox.getMinZ();
    }
}
