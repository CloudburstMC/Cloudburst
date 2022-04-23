package org.cloudburstmc.api.registry;

public interface GlobalRegistry {

    <T> BehaviorRegistry<T> getRegistry(Class<T> typeClass);
}
