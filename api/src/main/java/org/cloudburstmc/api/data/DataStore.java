package org.cloudburstmc.api.data;

public interface DataStore {

    <T> T getData(DataKey<T, ?> key);
}
