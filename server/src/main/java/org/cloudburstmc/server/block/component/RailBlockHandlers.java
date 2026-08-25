package org.cloudburstmc.server.block.component;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockStates;
import org.cloudburstmc.api.block.component.ComplexBlockHandler;
import org.cloudburstmc.api.block.component.NeighborBlockHandler;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.block.util.BlockSupport;
import org.cloudburstmc.server.block.util.RailConnector;
import org.cloudburstmc.server.level.CloudLevel;
import org.cloudburstmc.server.level.particle.DestroyBlockParticle;

@UtilityClass
public class RailBlockHandlers {

    public static final NeighborBlockHandler ON_NEIGHBOUR_CHANGED = (block, neighbor) -> {
        if (checkAndBreakIfUnsupported(block)) {
            return;
        }

        CloudLevel level = (CloudLevel) block.getLevel();
        Vector3i pos = block.getPosition();
        BlockState updated = RailConnector.updateDir(level, pos, block.getState(), false);

        if (updated != block.getState()) {
            level.setBlockState(pos, updated, true, false);
        }
    };

    public static final ComplexBlockHandler ON_REMOVE = block -> {
        CloudLevel level = (CloudLevel) block.getLevel();
        RailConnector.updateSurroundingRails(level, block.getPosition());
    };

    public static boolean isSupported(Block block) {
        CloudLevel level = (CloudLevel) block.getLevel();
        Vector3i pos = block.getPosition();
        BlockState state = block.getState();

        if (!RailConnector.hasRigidSupport(level, pos)) {
            return false;
        }

        if (RailConnector.getDirection(state).isAscending()) {
            Direction ascendFace = RailConnector.getDirection(state).ascendingDirection();
            if (ascendFace != null) {
                Vector3i ascendPos = ascendFace.relative(pos);
                return BlockSupport.canSupportRigidBlock(level, ascendPos);
            }
        }
        return true;
    }

    public static boolean checkAndBreakIfUnsupported(Block block) {
        if (isSupported(block)) {
            return false;
        }

        CloudLevel level = (CloudLevel) block.getLevel();
        Vector3i pos = block.getPosition();
        BlockState state = block.getState();

        DefaultBlockHandlers.dropLoot(block);

        level.addParticle(new DestroyBlockParticle(pos.toFloat().add(0.5f, 0.5f, 0.5f), state));
        block.set(BlockStates.AIR, false, true);
        return true;
    }
}
