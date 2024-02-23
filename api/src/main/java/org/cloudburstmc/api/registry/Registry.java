package org.cloudburstmc.api.registry;

public interface Registry<T> {

    void close() throws RegistryException;
}