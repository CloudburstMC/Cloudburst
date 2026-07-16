package org.cloudburstmc.server.registry;

import org.cloudburstmc.api.data.ComponentType;
import org.cloudburstmc.api.registry.RegistryException;
import org.cloudburstmc.server.registry.component.CloudComponentMap;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class CloudComponentRegistryTest {

    private static final ComponentType<Runnable> OPTIONAL = ComponentType.of("test:optional", Runnable.class);

    @Test
    void optionalComponentIsAbsentUntilConfigured() {
        TestRegistry registry = new TestRegistry();
        registry.registerComponent(OPTIONAL);
        CloudComponentMap components = registry.registerType();

        assertNull(components.get(OPTIONAL));

        Runnable implementation = () -> {
        };
        components.set(OPTIONAL, implementation);
        assertSame(implementation, components.get(OPTIONAL));
    }

    private static final class TestRegistry extends CloudComponentRegistry<String> {

        private CloudComponentMap registerType() {
            CloudComponentMap components = new CloudComponentMap(this);
            putComponents("type", components);
            return components;
        }

        @Override
        public void close() throws RegistryException {
            freezeComponentMaps();
        }
    }
}
