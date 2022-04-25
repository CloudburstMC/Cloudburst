package org.cloudburstmc.server.registry;

import org.cloudburstmc.api.registry.RegistryException;

public interface Registry {

    void close() throws RegistryException;
}
