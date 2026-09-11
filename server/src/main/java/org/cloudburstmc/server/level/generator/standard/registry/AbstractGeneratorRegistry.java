package org.cloudburstmc.server.level.generator.standard.registry;

import com.google.common.base.Preconditions;
import lombok.NonNull;
import org.cloudburstmc.api.event.Event;
import org.cloudburstmc.api.registry.Registry;
import org.cloudburstmc.api.registry.RegistryException;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.server.CloudServer;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Base class for registries used by the Cloudburst standard generator.
 */
public abstract class AbstractGeneratorRegistry<V> implements Registry {
    protected final Map<Identifier, Class<? extends V>> idToValues = new LinkedHashMap<>();

    private volatile boolean closed;

    public AbstractGeneratorRegistry() {
        this.registerDefault();

        Event event = this.constructionEvent();
        if (CloudServer.getInstance() != null) {
            CloudServer.getInstance().getEventManager().fire(event);
        }
        this.close();
    }

    public void register(@NonNull Identifier id, @NonNull Class<? extends V> clazz) {
        Preconditions.checkState(this.idToValues.putIfAbsent(id, clazz) == null, "ID \"%s\" already registered!", id);
    }

    public Class<? extends V> get(@NonNull Identifier id) {
        Preconditions.checkState(this.closed, "not closed");
        return Preconditions.checkNotNull(this.idToValues.get(id), id.toString());
    }

    public boolean isRegistered(@NonNull Identifier id) {
        Preconditions.checkState(this.closed, "not closed");
        return this.idToValues.containsKey(id);
    }

    @Override
    public synchronized void close() throws RegistryException {
        if (this.closed) {
            throw new RegistryException("already closed");
        }

        this.closed = true;
    }

    protected abstract void registerDefault();

    protected abstract Event constructionEvent();
}
