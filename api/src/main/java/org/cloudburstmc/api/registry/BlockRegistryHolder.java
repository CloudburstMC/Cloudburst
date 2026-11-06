package org.cloudburstmc.api.registry;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Iterator;
import java.util.ServiceLoader;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class BlockRegistryHolder {

    static final BlockRegistry INSTANCE = load();

    private static BlockRegistry load() {
        Iterator<BlockRegistry> registries = ServiceLoader.load(BlockRegistry.class, BlockRegistry.class.getClassLoader()).iterator();
        if (!registries.hasNext()) {
            throw new IllegalStateException("No block registry implementation is available");
        }

        BlockRegistry registry = registries.next();
        if (registries.hasNext()) {
            throw new IllegalStateException("Multiple block registry implementations are available");
        }

        return registry;
    }
}
