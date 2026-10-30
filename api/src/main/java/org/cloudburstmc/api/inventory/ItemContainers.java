package org.cloudburstmc.api.inventory;

import java.util.function.IntFunction;

/**
 * Creates detached item containers.
 *
 * <p>The API declares only the container type; the server registers a factory that builds
 * its own storage implementation. Callers that build data objects holding containers must
 * ensure the server registration has run, for example by calling the server's NBT entry
 * point first.</p>
 */
public final class ItemContainers {

    private static volatile IntFunction<ItemContainer> factory;

    private ItemContainers() {
    }

    /**
     * Creates a detached container with the given slot count.
     *
     * @param size the slot count
     * @return a new empty container
     * @throws IllegalStateException if no container factory has been registered
     */
    public static ItemContainer detached(int size) {
        IntFunction<ItemContainer> current = factory;
        if (current == null) {
            throw new IllegalStateException(
                    "No item container factory registered; call ItemContainers.setFactory first");
        }
        return current.apply(size);
    }

    /**
     * Registers the factory used by {@link #detached(int)}.
     *
     * <p>Passing {@code null} clears the registration; later {@code detached} calls fail
     * until a factory is set again.</p>
     *
     * @param factory the container factory, or {@code null} to clear it
     */
    public static void setFactory(IntFunction<ItemContainer> factory) {
        ItemContainers.factory = factory;
    }
}
