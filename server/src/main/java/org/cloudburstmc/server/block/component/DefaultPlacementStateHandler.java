package org.cloudburstmc.server.block.component;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockTraits;
import org.cloudburstmc.api.block.component.PlacementStateHandler;
import org.cloudburstmc.api.block.trait.BlockTrait;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.api.util.data.CardinalDirection;
import org.cloudburstmc.math.vector.Vector3f;

import java.util.Map;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class DefaultPlacementStateHandler implements PlacementStateHandler {

    public static final DefaultPlacementStateHandler INSTANCE = new DefaultPlacementStateHandler();

    @Override
    public BlockState execute(BlockState state, Block block, Player player, Direction face, Vector3f clickPosition) {
        if (player == null) {
            return state;
        }

        Map<BlockTrait<?>, Comparable<?>> traits = state.getTraits();
        if (traits.containsKey(BlockTraits.AXIS)) {
            state = state.withTrait(BlockTraits.AXIS, face.getAxis());
        }

        Direction oppositePlayerDirection = player.getHorizontalDirection().getOpposite();
        if (traits.containsKey(BlockTraits.CARDINAL_DIRECTION)) {
            state = state.withTrait(BlockTraits.CARDINAL_DIRECTION, oppositePlayerDirection.getCardinalDirection());
        }

        if (traits.containsKey(BlockTraits.DIRECTION)) {
            state = state.withTrait(BlockTraits.DIRECTION, oppositePlayerDirection);
        }

        if (traits.containsKey(BlockTraits.FACING_DIRECTION)) {
            state = state.withTrait(BlockTraits.FACING_DIRECTION, resolveFacingDirection(player));
        }

        if (traits.containsKey(BlockTraits.SIGN_DIRECTION)) {
            state = state.withTrait(BlockTraits.SIGN_DIRECTION, yawToCardinalDirection(player.getYaw()));
        }

        return state;
    }

    private static Direction resolveFacingDirection(Player player) {
        float pitch = player.getPitch();
        if (pitch < -45f) {
            return Direction.UP;
        }

        if (pitch > 45f) {
            return Direction.DOWN;
        }

        return player.getHorizontalDirection().getOpposite();
    }

    private static CardinalDirection yawToCardinalDirection(float yaw) {
        int index = Math.round(((yaw % 360f) + 360f) % 360f / 22.5f) % 16;
        return CardinalDirection.values()[index];
    }
}
