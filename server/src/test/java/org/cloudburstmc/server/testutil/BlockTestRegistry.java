package org.cloudburstmc.server.testutil;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.server.registry.CloudBlockRegistry;
import org.cloudburstmc.server.registry.CloudItemRegistry;

/**
 * Shared registry for tests using the globally bound built-in block types and palette.
 */
@UtilityClass
public class BlockTestRegistry {

    private static final CloudBlockRegistry REGISTRY = createRegistry();

    public static CloudBlockRegistry get() {
        return REGISTRY;
    }

    private static CloudBlockRegistry createRegistry() {
        CloudBlockRegistry registry = new CloudBlockRegistry(CloudItemRegistry.get());
        registry.close();
        return registry;
    }
}
