package org.cloudburstmc.server.registry;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.cloudburstmc.api.entity.damage.DamageType;
import org.cloudburstmc.api.entity.damage.DamageTypes;
import org.cloudburstmc.api.registry.DamageTypeRegistry;
import org.cloudburstmc.api.registry.RegistryException;
import org.cloudburstmc.api.util.Identifier;

import java.util.Collection;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CloudDamageTypeRegistry implements DamageTypeRegistry {

    private static final CloudDamageTypeRegistry INSTANCE = new CloudDamageTypeRegistry();

    private volatile boolean closed;

    public static CloudDamageTypeRegistry get() {
        return INSTANCE;
    }

    @Override
    public Optional<DamageType> get(Identifier id) {
        checkNotNull(id, "id");
        return DamageTypes.get(id);
    }

    @Override
    public Collection<DamageType> values() {
        return DamageTypes.values();
    }

    @Override
    public void close() throws RegistryException {
        checkState(!this.closed, "Registration is already closed");
        this.closed = true;
    }
}
