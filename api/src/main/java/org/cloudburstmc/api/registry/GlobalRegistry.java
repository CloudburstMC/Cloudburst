package org.cloudburstmc.api.registry;

public interface GlobalRegistry {

    <T> Registry<T> getRegistry(Class<T> typeClass);
}