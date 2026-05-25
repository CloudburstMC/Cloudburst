package org.cloudburstmc.server.block.component;

import org.cloudburstmc.api.block.BlockComponents;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockStates;
import org.cloudburstmc.api.block.BlockTraits;
import org.cloudburstmc.api.block.component.PlaceBlockHandler;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.registry.CloudBlockRegistry;

public class TorchPlaceHandler implements PlaceBlockHandler {

    private static final Direction[] FALLBACK_FACES = {
            Direction.SOUTH, Direction.WEST, Direction.NORTH, Direction.EAST, Direction.UP
    };

    private final CloudBlockRegistry registry;

    public TorchPlaceHandler(CloudBlockRegistry registry) {
        this.registry = registry;
    }

    @Override
    public boolean execute(BlockState blockState, Player player, Vector3i pos, Direction face, Vector3f clickPos) {
        Direction attachFace = face == Direction.DOWN ? findValidFace(player.getLevel(), pos) : face;
        if (attachFace == null || !isSupportingSolid(player.getLevel(), pos, attachFace)) {
            attachFace = findValidFace(player.getLevel(), pos);
        }

        if (attachFace == null) {
            return false;
        }

        Direction torchDirection = attachFace == Direction.UP ? Direction.DOWN : attachFace.getOpposite();
        blockState = blockState.withTrait(BlockTraits.TORCH_DIRECTION, torchDirection);
        return player.getLevel().setBlockState(pos, blockState, true, true);
    }

    private Direction findValidFace(Level level, Vector3i pos) {
        for (Direction candidate : FALLBACK_FACES) {
            if (isSupportingSolid(level, pos, candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private boolean isSupportingSolid(Level level, Vector3i pos, Direction attachFace) {
        Vector3i supportPos = attachFace.getOpposite().relative(pos);
        BlockState support = level.getBlockState(supportPos.getX(), supportPos.getY(), supportPos.getZ());
        if (support == BlockStates.AIR) {
            return false;
        }
        return registry.getComponents(support.getType()).get(BlockComponents.SOLID).get();
    }
}
