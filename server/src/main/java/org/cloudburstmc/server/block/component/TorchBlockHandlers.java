package org.cloudburstmc.server.block.component;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.block.BlockComponents;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockStates;
import org.cloudburstmc.api.block.BlockTraits;
import org.cloudburstmc.api.block.component.NeighborBlockHandler;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.level.CloudLevel;
import org.cloudburstmc.server.level.particle.DestroyBlockParticle;
import org.cloudburstmc.server.registry.CloudBlockRegistry;

import java.util.concurrent.ThreadLocalRandom;

@UtilityClass
public class TorchBlockHandlers {

    public static final NeighborBlockHandler ON_NEIGHBOUR_CHANGED = (block, neighbor) -> {
        BlockState state = block.getState();
        Direction torchDirection = state.ensureTrait(BlockTraits.TORCH_DIRECTION);
        Vector3i pos = block.getPosition();

        Vector3i supportPos = torchDirection == Direction.DOWN
                ? pos.sub(0, 1, 0)
                : torchDirection.relative(pos);

        if (!neighbor.getPosition().equals(supportPos)) {
            return;
        }

        CloudLevel level = (CloudLevel) block.getLevel();
        BlockState support = level.getBlockState(supportPos.getX(), supportPos.getY(), supportPos.getZ());

        boolean solid = CloudBlockRegistry.REGISTRY.getComponents(support.getType()).get(BlockComponents.SOLID).get();
        if (solid) {
            return;
        }

        ItemStack drop = CloudBlockRegistry.REGISTRY.getComponents(state.getType())
                .get(BlockComponents.GET_RESOURCE)
                .execute(block, ThreadLocalRandom.current(), 0);
        if (!drop.isEmpty()) {
            level.dropItem(pos.toFloat().add(0.5f, 0.5f, 0.5f), drop);
        }

        level.addParticle(new DestroyBlockParticle(pos.toFloat().add(0.5f, 0.5f, 0.5f), state));
        block.set(BlockStates.AIR, false, true);
    };
}
