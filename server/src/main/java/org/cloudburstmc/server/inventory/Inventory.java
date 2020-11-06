package org.cloudburstmc.server.inventory;

import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.player.Player;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public interface Inventory {

    int MAX_STACK = 64;


    int getSize();

    int getMaxStackSize();

    void setMaxStackSize(int size);

    String getName();

    String getTitle();

    ItemStack getItem(int index);

    default boolean setItem(int index, ItemStack item) {
        return setItem(index, item, true);
    }

    boolean setItem(int index, ItemStack item, boolean send);

    ItemStack[] addItem(ItemStack... slots);

    boolean canAddItem(ItemStack item);

    ItemStack[] removeItem(ItemStack... slots);

    Map<Integer, ItemStack> getContents();

    void setContents(Map<Integer, ItemStack> items);

    void sendContents(Player player);

    void sendContents(Player... players);

    void sendContents(Collection<Player> players);

    void sendSlot(int index, Player player);

    void sendSlot(int index, Player... players);

    void sendSlot(int index, Collection<Player> players);

    boolean contains(ItemStack item);

    Map<Integer, ItemStack> all(ItemStack item);

    default int first(ItemStack item) {
        return first(item, false);
    }

    /**
     * Search for the first occurrence of target item
     *
     * @param item  target item
     * @param exact if true the item count will be must match
     * @return the first index containing the item
     */
    int first(ItemStack item, boolean exact);

    default int firstFit(ItemStack item) {
        return firstFit(item, false);
    }

    /**
     * Returns the first slot where item fits to
     *
     * @param item   item to search for
     * @param single if false the item count will be used. Otherwise it'll be 1
     * @return the first slot index that item fits to
     */
    int firstFit(ItemStack item, boolean single);

    /**
     * Search for the first empty slot
     *
     * @return the first slot index
     */
    int firstEmpty();

    /**
     * Search for the first slot containing an item
     *
     * @return the first non-empty slot
     */
    int firstNonEmpty();

    /**
     * Returns how much space remains for the target item
     *
     * @param item target item
     * @return amount if free space
     */
    int getFreeSpace(ItemStack item);

    /**
     * Decrease item count in the given slot
     *
     * @param slot target slot index
     */
    void decrementCount(int slot);

    /**
     * Increase item count in the given slot
     *
     * @param slot target slot index
     */
    void incrementCount(int slot);

    void remove(ItemStack item);

    default boolean clear(int index) {
        return clear(index, true);
    }

    boolean clear(int index, boolean send);

    void clearAll();

    boolean isFull();

    boolean isEmpty();

    Set<Player> getViewers();

    InventoryType getType();

    InventoryHolder getHolder();

    void onOpen(Player who);

    boolean open(Player who);

    void close(Player who);

    void onClose(Player who);

    void onSlotChange(int index, ItemStack before, boolean send);
}
