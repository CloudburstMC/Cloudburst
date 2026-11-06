package org.cloudburstmc.api.inventory;

import java.util.List;
import java.util.ServiceLoader;
import java.util.stream.Collectors;

/**
 * Creates detached item containers.
 *
 * <p>The API declares only the container type; the storage comes from the single
 * {@link ItemContainerFactory} the server provides as a service. It is discovered on
 * first use, so data objects holding containers can be built before anything else
 * has run.</p>
 */
public final class ItemContainers {

    private static volatile ItemContainerFactory factory;

    private ItemContainers() {
    }

    /**
     * Creates a detached container with the given slot count.
     *
     * @param size the slot count
     * @return a new empty container
     * @throws IllegalStateException if no {@link ItemContainerFactory} service, or more
     *                               than one, is available
     */
    public static ItemContainer detached(int size) {
        return factory().create(size);
    }

    private static ItemContainerFactory factory() {
        ItemContainerFactory current = factory;
        if (current == null) {
            synchronized (ItemContainers.class) {
                current = factory;
                if (current == null) {
                    current = load();
                    factory = current;
                }
            }
        }
        return current;
    }

    private static ItemContainerFactory load() {
        List<ItemContainerFactory> found = ServiceLoader
                .load(ItemContainerFactory.class, ItemContainers.class.getClassLoader())
                .stream()
                .map(ServiceLoader.Provider::get)
                .toList();
        if (found.size() == 1) {
            return found.getFirst();
        }
        if (found.isEmpty()) {
            throw new IllegalStateException("No ItemContainerFactory service available; the server provides it in "
                    + "META-INF/services/" + ItemContainerFactory.class.getName());
        }
        throw new IllegalStateException("Expected one ItemContainerFactory service, found: " + found.stream()
                .map(provider -> provider.getClass().getName())
                .collect(Collectors.joining(", ")));
    }
}
