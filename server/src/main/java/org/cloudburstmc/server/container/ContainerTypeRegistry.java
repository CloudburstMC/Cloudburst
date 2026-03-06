package org.cloudburstmc.server.container;

import org.cloudburstmc.api.inventory.ScreenType;
import org.cloudburstmc.api.inventory.ScreenTypes;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerType;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Maps API {@link ScreenType} constants to the protocol
 * {@link ContainerType} values used in {@code ContainerOpenPacket}.
 */
public final class ContainerTypeRegistry {

    private static final Map<ScreenType<?>, ContainerType> REGISTRY = new IdentityHashMap<>();

    static {
        register(ScreenTypes.ANVIL, ContainerType.ANVIL);
        register(ScreenTypes.BARREL, ContainerType.CONTAINER);
        register(ScreenTypes.BEACON, ContainerType.BEACON);
        register(ScreenTypes.BLAST_FURNACE, ContainerType.BLAST_FURNACE);
        register(ScreenTypes.BREWING_STAND, ContainerType.BREWING_STAND);
        register(ScreenTypes.CARTOGRAPHY, ContainerType.CARTOGRAPHY);
        register(ScreenTypes.CHEST, ContainerType.CONTAINER);
        register(ScreenTypes.CRAFTER, ContainerType.CRAFTER);
        register(ScreenTypes.CRAFTING_TABLE, ContainerType.WORKBENCH);
        register(ScreenTypes.DISPENSER, ContainerType.DISPENSER);
        register(ScreenTypes.DOUBLE_CHEST, ContainerType.CONTAINER);
        register(ScreenTypes.DROPPER, ContainerType.DROPPER);
        register(ScreenTypes.ENCHANTING, ContainerType.ENCHANTMENT);
        register(ScreenTypes.ENDER_CHEST, ContainerType.CONTAINER);
        register(ScreenTypes.FURNACE, ContainerType.FURNACE);
        register(ScreenTypes.GRINDSTONE, ContainerType.GRINDSTONE);
        register(ScreenTypes.HOPPER, ContainerType.HOPPER);
        register(ScreenTypes.LECTERN, ContainerType.LECTERN);
        register(ScreenTypes.LOOM, ContainerType.LOOM);
        register(ScreenTypes.SHULKER_BOX, ContainerType.CONTAINER);
        register(ScreenTypes.SMITHING, ContainerType.SMITHING_TABLE);
        register(ScreenTypes.SMOKER, ContainerType.SMOKER);
        register(ScreenTypes.STONECUTTER, ContainerType.STONECUTTER);
        register(ScreenTypes.VIRTUAL_CHEST, ContainerType.CONTAINER);
        register(ScreenTypes.VIRTUAL_DOUBLE_CHEST, ContainerType.CONTAINER);
        register(ScreenTypes.VIRTUAL_HOPPER, ContainerType.HOPPER);
    }

    private ContainerTypeRegistry() {
    }

    private static void register(ScreenType<?> screenType, ContainerType containerType) {
        REGISTRY.put(screenType, containerType);
    }

    /**
     * Registers a custom {@link ScreenType} → {@link ContainerType} mapping.
     *
     * <p>Plugins that introduce custom virtual screens backed by a specific container
     * type should call this during server start-up to ensure that
     * {@link org.cloudburstmc.server.container.screen.CloudBlockContainerScreen} sends the correct
     * {@code ContainerOpenPacket} type for their screen.</p>
     *
     * <p>If a mapping for the given {@code screenType} already exists it will be silently
     * overwritten (last writer wins).</p>
     *
     * @param screenType    the screen type token (must be non-null, identity-keyed)
     * @param containerType the container type to send in the open packet
     */
    public static void registerCustom(ScreenType<?> screenType, ContainerType containerType) {
        Objects.requireNonNull(screenType, "screenType");
        Objects.requireNonNull(containerType, "containerType");
        REGISTRY.put(screenType, containerType);
    }

    /**
     * Returns the protocol {@link ContainerType} for the given screen type,
     * or {@link ContainerType#CONTAINER} as a fallback.
     */
    public static ContainerType get(ScreenType<?> screenType) {
        return REGISTRY.getOrDefault(screenType, ContainerType.CONTAINER);
    }
}
