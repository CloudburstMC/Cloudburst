package org.cloudburstmc.api.data;

public interface DataStore {

    <T> T get(DataKey<T, ?> key);
}
