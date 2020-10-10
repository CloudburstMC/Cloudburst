package org.cloudburstmc.server.level.generator.standard.registry;

import com.google.common.base.Preconditions;
import lombok.NonNull;
import org.cloudburstmc.api.registry.RegistryException;
import org.cloudburstmc.server.CloudServer;
import org.cloudburstmc.server.event.Event;
import org.cloudburstmc.server.registry.Registry;
import org.cloudburstmc.server.utils.Identifier;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

/**
 * Base class for registries used by the Cloudburst standard generator.
 *
 * @author DaPorkchop_
 */
public abstract class AbstractGeneratorRegistry<V> implements Registry {
    private static final AtomicIntegerFieldUpdater<AbstractGeneratorRegistry> CLOSED_UPDATER
            = AtomicIntegerFieldUpdater.newUpdater(AbstractGeneratorRegistry.class, "closed");

    protected final Map<Identifier, Class<? extends V>> idToValues = new IdentityHashMap<>();

    private volatile int closed = 0;

    public AbstractGeneratorRegistry() {
        this.registerDefault();

        Event event = this.constructionEvent();
        if (CloudServer.getInstance() != null) {
            //i was debugging stuff
            CloudServer.getInstance().getEventManager().fire(event);
        }
        this.close();
    }

    public void register(@NonNull Identifier id, @NonNull Class<? extends V> clazz) {
        Preconditions.checkState(this.idToValues.putIfAbsent(id, clazz) == null, "ID \"%s\" already registered!", id);
    }

    public Class<? extends V> get(@NonNull Identifier id) {
        Preconditions.checkState(this.closed == 1, "not closed");
        return Preconditions.checkNotNull(this.idToValues.get(id), id.toString());
    }

    public boolean isRegistered(@NonNull Identifier id) {
        Preconditions.checkState(this.closed == 1, "not closed");
        return this.idToValues.containsKey(id);
    }

    @Override
    public void close() throws RegistryException {
        if (!CLOSED_UPDATER.compareAndSet(this, 0, 1)) {
            throw new RegistryException("already closed");
        }
    }

    protected abstract void registerDefault();

    protected abstract Event constructionEvent();
}
