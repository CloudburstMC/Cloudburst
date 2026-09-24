package org.cloudburstmc.server.registry;

import org.cloudburstmc.api.level.particle.ParticleType;
import org.cloudburstmc.api.level.particle.ParticleTypes;
import org.cloudburstmc.server.network.NetworkUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;

class ParticleCatalogTest {
    @Test
    void resolvesEveryBuiltInParticleByIdentifier() {
        ParticleTypes.values().forEach(type -> assertSame(type, ParticleTypes.get(type.getId()).orElseThrow()));
    }

    @Test
    void mapsEveryNetworkParticleToItsBuiltInType() {
        for (org.cloudburstmc.protocol.bedrock.data.ParticleType networkType : org.cloudburstmc.protocol.bedrock.data.ParticleType.values()) {
            ParticleType type = NetworkUtils.particleFromNetwork(networkType);
            assertSame(networkType, NetworkUtils.particleToNetwork(type));
        }
    }
}
