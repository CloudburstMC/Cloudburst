package org.cloudburstmc.server.block.component;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockStates;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.block.component.AttackBlockHandler;
import org.cloudburstmc.api.block.component.UseBlockHandler;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.level.CloudLevel;
import org.cloudburstmc.server.level.particle.DragonEggTeleportParticle;

import java.util.concurrent.ThreadLocalRandom;

@UtilityClass
public class DragonEggBlockHandlers {

    private static final int TELEPORT_ATTEMPTS = 1000;
    private static final int HORIZONTAL_RANGE = 16;
    private static final int VERTICAL_RANGE = 8;

    public static final AttackBlockHandler ATTACK = (block, player, direction) -> {
        teleport(block);
        return true;
    };

    public static final UseBlockHandler USE = (block, player, direction, item) -> {
        teleport(block);
        return true;
    };

    private static void teleport(Block block) {
        CloudLevel level = (CloudLevel) block.getLevel();
        Vector3i origin = block.getPosition();
        BlockState state = block.getState();
        ThreadLocalRandom random = ThreadLocalRandom.current();

        for (int attempt = 0; attempt < TELEPORT_ATTEMPTS; attempt++) {
            Vector3i destination = origin.add(
                    random.nextInt(HORIZONTAL_RANGE) - random.nextInt(HORIZONTAL_RANGE),
                    random.nextInt(VERTICAL_RANGE) - random.nextInt(VERTICAL_RANGE),
                    random.nextInt(HORIZONTAL_RANGE) - random.nextInt(HORIZONTAL_RANGE)
            );

            if (destination.getY() < level.getMinHeight() || destination.getY() >= level.getMaxHeight()
                    || level.getBlockState(destination).getType() != BlockTypes.AIR
                    || level.getBlockState(destination.sub(0, 1, 0)).getType() == BlockTypes.AIR) {
                continue;
            }

            level.addParticle(new DragonEggTeleportParticle(origin, destination),
                    level.getChunk(origin).getViewers().toArray(Player[]::new));
            level.setBlockState(destination, state);
            level.setBlockState(origin, BlockStates.AIR);
            return;
        }
    }
}
