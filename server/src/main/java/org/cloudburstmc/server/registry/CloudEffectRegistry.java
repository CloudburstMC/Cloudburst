package org.cloudburstmc.server.registry;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.cloudburstmc.api.potion.EffectType;
import org.cloudburstmc.api.potion.EffectTypes;
import org.cloudburstmc.api.registry.EffectRegistry;
import org.cloudburstmc.api.registry.RegistryException;
import org.cloudburstmc.api.util.Identifier;

import java.util.Collection;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CloudEffectRegistry implements EffectRegistry {
    private static final CloudEffectRegistry INSTANCE = new CloudEffectRegistry();

    private volatile boolean closed;

    public static CloudEffectRegistry get() {
        return INSTANCE;
    }

    @Override
    public Optional<EffectType> get(Identifier id) {
        checkNotNull(id, "id");
        return EffectTypes.get(id);
    }

    @Override
    public Collection<EffectType> values() {
        return EffectTypes.values();
    }

    @Override
    public void close() throws RegistryException {
        checkState(!this.closed, "Registration is already closed");
        this.closed = true;
    }
}
