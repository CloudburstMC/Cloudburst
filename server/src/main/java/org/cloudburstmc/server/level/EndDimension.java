package org.cloudburstmc.server.level;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.blockentity.EndGateway;
import org.cloudburstmc.api.entity.EntityTypes;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.entity.misc.EntityEnderCrystal;
import org.cloudburstmc.server.level.chunk.CloudChunk;
import org.cloudburstmc.server.level.feature.EndGatewayFeature;
import org.cloudburstmc.server.level.generator.standard.population.EndSpikeLayout;
import org.cloudburstmc.server.registry.CloudEntityRegistry;

@UtilityClass
public class EndDimension {

    private static final Vector3i RETURN_GATEWAY_EXIT = Vector3i.from(100, 50, 0);

    public static void onChunkGenerated(CloudLevel level, CloudChunk chunk) {
        if (level.getDimension() != CloudLevel.DIMENSION_THE_END) {
            return;
        }

        bindReturnGateways(level, chunk);
        for (EndSpikeLayout.Spike spike : EndSpikeLayout.create(level.getSeed())) {
            if (!spike.isInChunk(chunk.getX(), chunk.getZ())) {
                continue;
            }

            if (level.getBlockState(spike.x(), spike.height(), spike.z()).getType() != BlockTypes.BEDROCK) {
                continue;
            }

            spawnSpikeCrystal(level, spike);
        }
    }

    public static EntityEnderCrystal spawnSpikeCrystal(CloudLevel level, EndSpikeLayout.Spike spike) {
        Location location = Location.from(Vector3f.from(spike.x() + 0.5f, spike.height() + 1, spike.z() + 0.5f), level);
        EntityEnderCrystal crystal = (EntityEnderCrystal) CloudEntityRegistry.get().newEntity(EntityTypes.ENDER_CRYSTAL, location);
        crystal.setShowingBase(true);
        crystal.spawnToAll();
        return crystal;
    }

    private static void bindReturnGateways(CloudLevel level, CloudChunk chunk) {
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int highestY = chunk.getHighestBlock(x, z);
                int gatewayY = highestY - 2;
                if (gatewayY < level.getMinHeight() || chunk.getBlockState(x, gatewayY, z).getType() != BlockTypes.END_GATEWAY) {
                    continue;
                }

                Vector3i position = Vector3i.from((chunk.getX() << 4) + x, gatewayY, (chunk.getZ() << 4) + z);
                if (!EndGateways.isOutsideCentralIsland(position)) {
                    continue;
                }

                if (level.getBlockEntity(position) instanceof EndGateway) {
                    continue;
                }

                EndGatewayFeature.bindBlockEntity(level, position, RETURN_GATEWAY_EXIT, true);
            }
        }
    }
}
