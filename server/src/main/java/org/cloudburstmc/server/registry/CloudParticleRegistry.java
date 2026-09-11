package org.cloudburstmc.server.registry;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.cloudburstmc.api.level.particle.ParticleType;
import org.cloudburstmc.api.level.particle.ParticleTypes;
import org.cloudburstmc.api.registry.ParticleRegistry;
import org.cloudburstmc.api.registry.RegistryException;
import org.cloudburstmc.api.util.Identifier;

import java.util.Collection;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CloudParticleRegistry implements ParticleRegistry {
    private static final CloudParticleRegistry INSTANCE = new CloudParticleRegistry();

    private volatile boolean closed;

    public static CloudParticleRegistry get() {
        return INSTANCE;
    }

    @Override
    public Optional<ParticleType> get(Identifier id) {
        checkNotNull(id, "id");
        return ParticleTypes.get(id);
    }

    @Override
    public Collection<ParticleType> values() {
        return ParticleTypes.values();
    }

    @Override
    public void close() throws RegistryException {
        checkState(!this.closed, "Registration is already closed");
        this.closed = true;
    }
}
