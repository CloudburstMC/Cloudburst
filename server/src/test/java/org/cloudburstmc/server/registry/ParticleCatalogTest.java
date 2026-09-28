package org.cloudburstmc.server.registry;

import org.cloudburstmc.api.level.particle.ParticleType;
import org.cloudburstmc.api.level.particle.ParticleTypes;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.server.network.NetworkUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ParticleCatalogTest {

    @Test
    void resolvesEveryBuiltInParticleByIdentifier() {
        ParticleTypes.values().forEach(type -> assertSame(type, ParticleTypes.get(type.getId()).orElseThrow()));
    }

    @Test
    void mapsEveryNetworkParticleToItsBuiltInType() {
        for (org.cloudburstmc.protocol.bedrock.data.ParticleType networkType : org.cloudburstmc.protocol.bedrock.data.ParticleType.values()) {
            if (networkType == org.cloudburstmc.protocol.bedrock.data.ParticleType.UNDEFINED) {
                assertThrows(IllegalArgumentException.class, () -> NetworkUtils.particleFromNetwork(networkType));
                continue;
            }

            ParticleType type = NetworkUtils.particleFromNetwork(networkType);
            assertSame(networkType, NetworkUtils.particleToNetwork(type));
            assertSame(networkType, NetworkUtils.particleToNetwork(ParticleType.of(type.getId())));
        }
    }

    @Test
    void rejectsEmitterIdentifiersInTheBuiltInParticlePath() {
        assertThrows(IllegalArgumentException.class, () -> NetworkUtils.particleToNetwork(ParticleType.of(Identifier.parse("minecraft:breeze_ground_particle"))));
    }
}
