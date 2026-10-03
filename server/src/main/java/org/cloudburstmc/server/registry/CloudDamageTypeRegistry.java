package org.cloudburstmc.server.registry;

import org.cloudburstmc.api.entity.damage.DamageType;
import org.cloudburstmc.api.entity.damage.DamageTypes;
import org.cloudburstmc.api.registry.DamageTypeRegistry;
import org.cloudburstmc.api.registry.RegistryException;
import org.cloudburstmc.api.util.Identifier;

import java.util.*;

import static com.google.common.base.Preconditions.*;

public class CloudDamageTypeRegistry implements DamageTypeRegistry {

    private static final CloudDamageTypeRegistry INSTANCE = new CloudDamageTypeRegistry();

    private volatile boolean closed;
    private final Map<Identifier, DamageType> types = new LinkedHashMap<>();

    private CloudDamageTypeRegistry() {
        for (DamageType type : DamageTypes.values()) {
            this.register(type);
        }
    }

    public static CloudDamageTypeRegistry get() {
        return INSTANCE;
    }

    @Override
    public synchronized Optional<DamageType> get(Identifier id) {
        checkNotNull(id, "id");
        return Optional.ofNullable(this.types.get(id));
    }

    @Override
    public synchronized Identifier getId(DamageType type) {
        checkNotNull(type, "type");
        checkArgument(this.types.get(type.getId()) == type, "Unregistered damage type: %s", type);
        return type.getId();
    }

    @Override
    public synchronized Collection<DamageType> values() {
        return List.copyOf(this.types.values());
    }

    @Override
    public synchronized void register(DamageType type) {
        checkNotNull(type, "type");
        checkState(!this.closed, "Registration is closed");
        checkArgument(!this.types.containsKey(type.getId()), "Damage type already registered: %s", type.getId());
        this.types.put(type.getId(), type);
    }

    @Override
    public synchronized void close() throws RegistryException {
        checkState(!this.closed, "Registration is already closed");
        this.closed = true;
    }
}
