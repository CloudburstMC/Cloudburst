package org.cloudburstmc.server.block.component;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockType;
import org.cloudburstmc.api.block.component.TickBlockHandler;
import org.cloudburstmc.api.event.block.BlockFormEvent;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.registry.VanillaCopperBlocks;

import java.util.random.RandomGenerator;

@UtilityClass
public class CopperWeatheringBlockHandlers {

    private static final float ATTEMPT_CHANCE = 0.05688889f;
    private static final int SCAN_DISTANCE = 4;

    public static final TickBlockHandler ON_RANDOM_TICK = (block, random) -> {
        BlockState original = block.getState();
        BlockType next = VanillaCopperBlocks.next(original.getType());
        if (next == null || random.nextFloat() >= ATTEMPT_CHANCE) {
            return;
        }

        int stage = VanillaCopperBlocks.weatheringStage(original.getType());
        if (!shouldWeather(block, stage, random)) {
            return;
        }

        BlockState replacement = next.getDefaultState().copyTraits(original);
        BlockFormEvent event = new BlockFormEvent(block, replacement);
        block.getLevel().getServer().getEventManager().fire(event);

        if (!event.isCancelled() && block.getLevel().getBlockState(block.getPosition()) == original) {
            block.set(event.getNewState(), false, true);
        }
    };

    private static boolean shouldWeather(Block block, int stage, RandomGenerator random) {
        Vector3i position = block.getPosition();
        int same = 0;
        int older = 0;

        for (int x = -SCAN_DISTANCE; x <= SCAN_DISTANCE; x++) {
            int yzDistance = SCAN_DISTANCE - Math.abs(x);
            for (int y = -yzDistance; y <= yzDistance; y++) {
                int zDistance = yzDistance - Math.abs(y);
                for (int z = -zDistance; z <= zDistance; z++) {
                    if (x == 0 && y == 0 && z == 0) {
                        continue;
                    }

                    Block nearby = block.getLevel().getLoadedBlock(position.add(x, y, z));
                    if (nearby == null) {
                        continue;
                    }

                    int nearbyStage = VanillaCopperBlocks.weatheringStage(nearby.getState().getType());
                    if (nearbyStage < 0) {
                        continue;
                    }

                    if (nearbyStage < stage) {
                        return false;
                    }

                    if (nearbyStage == stage) {
                        same++;
                    } else {
                        older++;
                    }
                }
            }
        }

        float ratio = (older + 1f) / (older + same + 1f);
        float modifier = stage == 0 ? 0.75f : 1;
        return random.nextFloat() < ratio * ratio * modifier;
    }
}
