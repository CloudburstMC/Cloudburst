package org.cloudburstmc.server.entity.data;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataMap;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataType;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag;

import java.util.*;

import static org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes.FLAGS;
import static org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes.FLAGS_2;

/**
 * Tracks entity metadata on the owning tick thread. Snapshots never consume pending changes.
 */
public class SyncedEntityData {

    private final EntityDataMap values;
    private final Set<EntityDataType<?>> dirtyTypes;
    private final EnumMap<EntityFlag, Boolean> flags;
    private boolean flagsDirty;

    public SyncedEntityData() {
        this.values = new EntityDataMap();
        this.dirtyTypes = new LinkedHashSet<>();
        this.flags = new EnumMap<>(EntityFlag.class);
    }

    public boolean contains(EntityDataType<?> type) {
        requireValueType(type);
        return this.values.containsKey(type);
    }

    /**
     * Returns a stored value, or {@code null} when the field has not been initialized.
     */
    public <T> @Nullable T get(EntityDataType<T> type) {
        requireValueType(type);
        return this.values.get(type);
    }

    /**
     * Returns an initialized field.
     *
     * @throws IllegalStateException if the field has not been initialized
     */
    public <T> T require(EntityDataType<T> type) {
        T value = this.get(type);
        if (value == null) {
            throw new IllegalStateException("Entity metadata has not been initialized: " + type);
        }
        return value;
    }

    /**
     * Stores a value and marks changed fields dirty. Text is stored as an immutable string.
     */
    public <T> void set(EntityDataType<T> type, T value) {
        requireValueType(type);
        Objects.requireNonNull(value, "value");
        Object storedValue = value instanceof CharSequence text ? text.toString() : value;
        if (!Objects.equals(this.values.get(type), storedValue)) {
            this.values.put(type, storedValue);
            this.dirtyTypes.add(type);
        }
    }

    public boolean getFlag(EntityFlag flag) {
        return this.flags.getOrDefault(Objects.requireNonNull(flag, "flag"), false);
    }

    public void setFlag(EntityFlag flag, boolean value) {
        Objects.requireNonNull(flag, "flag");
        Boolean previous = this.flags.put(flag, value);
        this.flagsDirty |= previous == null || previous != value;
    }

    /**
     * Returns detached metadata for the requested fields, or all metadata when no fields are supplied.
     * Requesting either flag field includes both flag fields.
     */
    public EntityDataMap snapshot(EntityDataType<?>... types) {
        Objects.requireNonNull(types, "types");

        EntityDataMap snapshot = new EntityDataMap();
        if (types.length == 0) {
            snapshot.putAll(this.values);
            this.putFlags(snapshot);
        } else {
            for (EntityDataType<?> type : types) {
                Objects.requireNonNull(type, "type");
                if (type == FLAGS || type == FLAGS_2) {
                    this.putFlags(snapshot);
                } else {
                    this.putValue(snapshot, type);
                }
            }
        }

        return snapshot;
    }

    /**
     * Returns detached changed metadata and clears its dirty markers before it can be dispatched.
     * An empty result means there are no pending changes.
     */
    public EntityDataMap drainChanges() {
        EntityDataMap changes = new EntityDataMap();
        for (EntityDataType<?> type : this.dirtyTypes) {
            this.putValue(changes, type);
        }

        if (this.flagsDirty) {
            this.putFlags(changes);
        }

        this.dirtyTypes.clear();
        this.flagsDirty = false;
        return changes;
    }

    private void putValue(EntityDataMap destination, EntityDataType<?> type) {
        Object value = this.values.get(type);
        if (value == null) {
            throw new IllegalArgumentException("Entity metadata has not been set: " + type);
        }

        destination.put(type, value);
    }

    private void putFlags(EntityDataMap destination) {
        destination.putFlags(new EnumMap<>(this.flags));
    }

    private static void requireValueType(EntityDataType<?> type) {
        Objects.requireNonNull(type, "type");
        if (type == FLAGS || type == FLAGS_2) {
            throw new IllegalArgumentException("Entity flags must be accessed through getFlag and setFlag");
        }
    }
}
