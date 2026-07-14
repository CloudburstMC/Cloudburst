package org.cloudburstmc.server.block.component;

import org.cloudburstmc.api.block.BlockComponents;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockTraits;
import org.cloudburstmc.api.block.component.PlaceBlockHandler;
import org.cloudburstmc.api.block.trait.BlockTrait;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.util.CollisionContext;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.api.util.VoxelShape;
import org.cloudburstmc.api.util.component.ComponentMap;
import org.cloudburstmc.api.util.data.CardinalDirection;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.registry.CloudBlockRegistry;

import java.util.Map;

public class DefaultBlockPlaceHandler implements PlaceBlockHandler {

    private final CloudBlockRegistry registry;

    public DefaultBlockPlaceHandler(CloudBlockRegistry registry) {
        this.registry = registry;
    }

    @Override
    public boolean execute(BlockState blockState, Player player, Vector3i blockPosition, Direction face, Vector3f clickPosition) {
        if (player == null) {
            return false;
        }

        Level level = player.getLevel();
        ComponentMap blockComponents = this.registry.getComponents(blockState.getType());
        VoxelShape collisionShape = blockComponents.get(BlockComponents.GET_COLLISION_SHAPE).execute(blockState, CollisionContext.of(player));

        if (!collisionShape.isEmpty() && level.hasEntityCollision(null, collisionShape, blockPosition)) {
            return false;
        }

        blockState = this.applyDirectionTraits(blockState, player, blockPosition, face);
        return level.setBlockState(blockPosition, blockState, true, true);
    }

    protected BlockState applyDirectionTraits(BlockState blockState, Player player, Vector3i blockPosition, Direction face) {
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
            blockState = blockState.withTrait(BlockTraits.FACING_DIRECTION, this.resolveFacingDirection(player));
        }

        if (traits.containsKey(BlockTraits.SIGN_DIRECTION)) {
            blockState = blockState.withTrait(BlockTraits.SIGN_DIRECTION, this.yawToCardinalDirection(player.getYaw()));
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
}
