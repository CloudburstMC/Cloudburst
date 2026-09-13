package org.cloudburstmc.server.level;

import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import org.cloudburstmc.api.entity.Entity;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Maintains the stable insertion order of entities scheduled to tick.
 */
final class EntityTickList {

    private Long2ObjectMap<Entity> active = new Long2ObjectLinkedOpenHashMap<>();
    private Long2ObjectMap<Entity> passive = new Long2ObjectLinkedOpenHashMap<>();
    private Long2ObjectMap<Entity> iterated;

    public synchronized int size() {
        return this.active.size();
    }

    public synchronized boolean isEmpty() {
        return this.active.isEmpty();
    }

    public synchronized void add(Entity entity) {
        Objects.requireNonNull(entity, "entity");
        this.ensureActiveIsNotIterated();
        this.active.put(entity.getUniqueId(), entity);
    }

    public synchronized void remove(Entity entity) {
        Objects.requireNonNull(entity, "entity");
        this.ensureActiveIsNotIterated();
        if (this.active.get(entity.getUniqueId()) == entity) {
            this.active.remove(entity.getUniqueId());
        }
    }

    public void forEach(Consumer<Entity> action) {
        Objects.requireNonNull(action, "action");

        Long2ObjectMap<Entity> entities;
        synchronized (this) {
            if (this.iterated != null) {
                throw new IllegalStateException("Entity tick list is already being iterated");
            }

            this.iterated = this.active;
            entities = this.active;
        }

        try {
            for (Entity entity : entities.values()) {
                action.accept(entity);
            }
        } finally {
            synchronized (this) {
                this.iterated = null;
            }
        }
    }

    private void ensureActiveIsNotIterated() {
        if (this.iterated != this.active) {
            return;
        }

        this.passive.clear();
        this.passive.putAll(this.active);

        Long2ObjectMap<Entity> previous = this.active;
        this.active = this.passive;
        this.passive = previous;
    }
}
