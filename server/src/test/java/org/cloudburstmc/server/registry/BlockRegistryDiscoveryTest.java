package org.cloudburstmc.server.registry;

import org.cloudburstmc.api.registry.BlockRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.ServiceLoader;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BlockRegistryDiscoveryTest {

    @Test
    void runtimeResourcesDeclareExactlyOneBlockRegistryImplementation() {
        List<Class<? extends BlockRegistry>> providers = ServiceLoader.load(BlockRegistry.class,
                        BlockRegistry.class.getClassLoader())
                .stream()
                .<Class<? extends BlockRegistry>>map(ServiceLoader.Provider::type)
                .toList();

        assertEquals(List.of(CloudBlockRegistry.class), providers);
    }
}
