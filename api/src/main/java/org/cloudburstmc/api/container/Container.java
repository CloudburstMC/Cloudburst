package org.cloudburstmc.api.container;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.cloudburstmc.api.item.ItemStack;

import java.util.Map;
import java.util.function.ObjIntConsumer;

public interface Container {

    int MAX_STACK = 64;

    int size();

    int getMaxStackSize();

    String getName();

    String getTitle();

    @NonNull
    ItemStack getItem(int index);

    /**
     * Set item to the slot
     *
     * @param index slot index
     * @param item  item to set
     */
    void setItem(int index, @NonNull ItemStack item);

    ItemStack[] addItem(ItemStack... slots);

    boolean canAddItem(ItemStack item);

    ItemStack[] removeItem(ItemStack... slots);

    ItemStack[] getContents();

    void setContents(ItemStack[] items);

    void refresh();

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

    void clear();

    boolean isFull();

    boolean isEmpty();

    void forEachSlot(ObjIntConsumer<ItemStack> consumer);
}
