package org.cloudburstmc.server.level.weather;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockStates;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.event.block.BlockFormEvent;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.block.util.BlockSupport;
import org.cloudburstmc.server.level.CloudLevel;
import org.cloudburstmc.server.level.biome.CloudBiome;
import org.cloudburstmc.server.registry.CloudBiomeRegistry;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PrecipitationHandler {

    private static final float FREEZING_TEMPERATURE = 0.15f;
    private static final int MAX_FORMATION_LIGHT = 9;

    public static void tickColumn(CloudLevel level, int x, int z) {
        int surfaceY = level.getHighestBlock(x, z);
        CloudBiome biome = CloudBiomeRegistry.get().getBiome(level.getBiomeId(x, surfaceY, z));
        if (biome == null) {
            return;
        }

        Block surface = level.getBlock(x, surfaceY, z);
        if (isColdEnough(biome, x, surfaceY, z)
                && surface.getState().getType() == BlockTypes.WATER
                && level.getBlockLightAt(x, surfaceY, z) <= MAX_FORMATION_LIGHT) {
            formBlock(level, surface, BlockStates.ICE);
        }

        if (!level.isRaining()) {
            return;
        }

        int targetY = surface.getState().getType() == BlockTypes.SNOW_LAYER ? surfaceY : surfaceY + 1;
        if (!isColdEnough(biome, x, targetY, z) || level.getBlockLightAt(x, targetY, z) > MAX_FORMATION_LIGHT) {
            return;
        }

        Block target = level.getBlock(x, targetY, z);
        BlockState state = target.getState();
        if (state == BlockStates.AIR && BlockSupport.isFaceSturdy(level, Vector3i.from(x, targetY - 1, z), Direction.UP)) {
            formBlock(level, target, BlockStates.SNOW_LAYER);
        }
    }

    private static boolean isColdEnough(CloudBiome biome, int x, int y, int z) {
        return biome.getTemperature(x, y, z) < FREEZING_TEMPERATURE;
    }

    private static void formBlock(CloudLevel level, Block block, BlockState state) {
        BlockFormEvent event = new BlockFormEvent(block, state);
        level.getServer().getEventManager().fire(event);
        if (!event.isCancelled()) {
            block.set(event.getNewState(), false, true);
        }
    }
}
