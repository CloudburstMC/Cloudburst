package org.cloudburstmc.api.inventory;

import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.inventory.view.SlotGroup;
import org.cloudburstmc.api.inventory.view.SlotGroupType;
import org.cloudburstmc.api.player.Player;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Represents an inventory screen currently open for a player (the full window being
 * displayed, which may comprise multiple {@link SlotGroup}s (for example a
 * chest's storage section alongside the player's own inventory).
 *
 * <p>Typed sub-interfaces such as {@link StorageScreen}, {@link FurnaceScreen}, and
 * {@link AnvilScreen} provide convenient accessors for the slot groups relevant to each
 * container type.</p>
 */
public interface InventoryScreen {

    /**
     * Returns the player this inventory screen is open for.
     *
     * @return the player viewing this screen
     */
    Player getPlayer();

    /**
     * Returns the title displayed in the GUI title bar for this screen.
     *
     * <p>For block container screens (chest, furnace, etc.) this returns the block entity's
     * custom name if one is set, or {@code null} otherwise. A plugin-supplied title override
     * set via {@link org.cloudburstmc.api.event.inventory.InventoryOpenEvent#setTitleOverride}
     * takes precedence over the default.</p>
     *
     * <p>For virtual container screens the title is set at construction time and can be
     * changed before opening via
     * {@link org.cloudburstmc.api.inventory.VirtualContainerScreen#setTitle(Component)}.</p>
     *
     * <p>For HUD and player-inventory screens this always returns {@code null}.</p>
     *
     * @return the title of this screen, or {@code null} if this screen has no title
     */
    @Nullable
    Component getTitle();

    /**
     * Retrieves the type of this inventory screen.
     *
     * @return the screen type
     */
    ScreenType<?> getType();

    /**
     * Retrieves the slot group types present in this screen.
     *
     * @return an unmodifiable set of the slot group types
     */
    Set<SlotGroupType<?>> getSlotGroupTypes();

    /**
     * Retrieves a slot group that is present in this screen.
     *
     * @param type the type of slot group
     * @param <T>  the slot group interface type
     * @return the slot group, if present
     */
    <T extends SlotGroup> Optional<T> getSlots(SlotGroupType<T> type);

    default <T extends SlotGroup> T getSlotsOrThrow(SlotGroupType<T> type) {
        return getSlots(type).orElseThrow(() -> new NoSuchElementException("Slot group type " + type + " is not present in this screen"));
    }

    /**
     * Returns all slot groups present in this screen.
     *
     * @return an unmodifiable set of all slot groups in this screen
     */
    Set<SlotGroup> getAllSlotGroups();

    /**
     * Returns all slot groups in this screen that are an instance of the given type.
     *
     * <pre>{@code
     * Set<BlockSlotGroup<?>> blockGroups = screen.getAllSlotGroups(BlockSlotGroup.class);
     * }</pre>
     *
     * @param type the class to filter by
     * @param <T>  the slot group subtype
     * @return an unmodifiable set of all matching slot groups, possibly empty
     */
    default <T extends SlotGroup> Set<T> getAllSlotGroups(Class<T> type) {
        return getAllSlotGroups().stream()
                .filter(type::isInstance)
                .map(type::cast)
                .collect(Collectors.toUnmodifiableSet());
    }
}
