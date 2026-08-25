package org.cloudburstmc.server.block.component;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.block.*;
import org.cloudburstmc.api.block.component.NeighborBlockHandler;
import org.cloudburstmc.api.block.component.PlayerBlockHandler;
import org.cloudburstmc.api.block.component.TickBlockHandler;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.block.util.PlacementSupport;
import org.cloudburstmc.server.block.util.TripwireCalculator;
import org.cloudburstmc.server.level.CloudLevel;
import org.cloudburstmc.server.level.particle.DestroyBlockParticle;

@UtilityClass
public class TripwireHookBlockHandlers {

    public static final TickBlockHandler ON_TICK = (block, random) -> {
        CloudLevel level = (CloudLevel) block.getLevel();
        Vector3i pos = block.getPosition();
        BlockState state = level.getBlockState(pos.getX(), pos.getY(), pos.getZ());
        TripwireCalculator.calculateState(level, pos, state, false, true);
    };

    public static final NeighborBlockHandler ON_NEIGHBOUR_CHANGED = (block, neighbor) -> {
        BlockState state = block.getState();
        Direction facing = state.ensureTrait(BlockTraits.DIRECTION);
        Vector3i pos = block.getPosition();
        Vector3i wallPos = PlacementSupport.supportPosition(pos, facing);

        if (neighbor.getPosition().equals(wallPos)) {
            CloudLevel level = (CloudLevel) block.getLevel();
            if (!PlacementSupport.hasFullFaceSupport(level, pos, facing)) {
                breakHook(block, state, level, pos);
            }
        }
    };

    public static final PlayerBlockHandler ON_DESTROY = (block, player) -> {
        BlockState state = block.getState();
        CloudLevel level = (CloudLevel) block.getLevel();
        Vector3i pos = block.getPosition();

        block.set(BlockStates.AIR, true, false);

        if (state.ensureTrait(BlockTraits.IS_ATTACHED) || state.ensureTrait(BlockTraits.IS_POWERED)) {
            TripwireCalculator.calculateState(level, pos, state, true, false);
        }

        if (state.ensureTrait(BlockTraits.IS_POWERED)) {
            TripwireCalculator.notifyRedstoneNeighbours(level, pos, state.ensureTrait(BlockTraits.DIRECTION));
        }

        level.updateAround(pos);
    };

    private void breakHook(Block block, BlockState state, CloudLevel level, Vector3i pos) {
        level.addParticle(new DestroyBlockParticle(pos.toFloat().add(0.5f, 0.5f, 0.5f), state));

        DefaultBlockHandlers.dropLoot(block);

        block.set(BlockStates.AIR, true, false);

        if (state.ensureTrait(BlockTraits.IS_ATTACHED) || state.ensureTrait(BlockTraits.IS_POWERED)) {
            TripwireCalculator.calculateState(level, pos, state, true, false);
        }

        if (state.ensureTrait(BlockTraits.IS_POWERED)) {
            TripwireCalculator.notifyRedstoneNeighbours(level, pos, state.ensureTrait(BlockTraits.DIRECTION));
        }

        level.updateAround(pos);
    }
}
