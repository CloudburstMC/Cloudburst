package org.cloudburstmc.server.container;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.cloudburstmc.api.item.ItemStack;

import java.util.Map;
import java.util.function.ObjIntConsumer;

/**
 * Low-level abstraction for a fixed-size slot array backing a block entity or virtual container.
 *
 * <p>Slots are addressed by zero-based index. Empty slots are represented by {@link ItemStack#EMPTY};
 * implementations should not expose {@code null} items from {@link #getItem(int)} or {@link #getContents()}.
 * Methods that search by item use {@link ItemStack#isStackableWith(ItemStack)}.</p>
 */
public interface Container {

    /**
     * Default maximum stack size used by containers without a custom cap.
     */
    int MAX_STACK = 64;

    /**
     * Returns the number of slots in this container.
     *
     * @return the fixed slot count
     */
    int size();

    /**
     * Returns this container's maximum stack size cap.
     *
     * <p>Item-specific stack caps still apply. Transfer operations use the smaller of this value and the
     * item's registered maximum stack size.</p>
     *
     * @return the container stack size cap
     */
    int getMaxStackSize();

    /**
     * Returns the container's internal name.
     *
     * @return the internal name, or an empty string when unnamed
     */
    String getName();

    /**
     * Returns the title shown to viewers.
     *
     * @return the display title, or an empty string when untitled
     */
    String getTitle();

    /**
     * Returns the item currently stored in the given slot.
     *
     * @param index zero-based slot index
     * @return the slot item, or {@link ItemStack#EMPTY} when the slot is empty
     * @throws IndexOutOfBoundsException if the index is outside this container's slot range
     */
    @NonNull
    ItemStack getItem(int index);

    /**
     * Stores an item in the given slot.
     *
     * @param index zero-based slot index
     * @param item  item to store, or {@link ItemStack#EMPTY} to clear the slot
     * @throws NullPointerException      if {@code item} is null
     * @throws IndexOutOfBoundsException if the index is outside this container's slot range
     */
    void setItem(int index, @NonNull ItemStack item);

    /**
     * Adds the given item stacks, merging with existing stackable items before using empty slots.
     *
     * <p>The operation is best-effort: earlier stacks may be inserted even when later stacks do not fit.
     * Returned leftovers keep the remaining item counts.</p>
     *
     * @param slots the stacks to add
     * @return any stack remainders that could not fit, or an empty array if all stacks were stored
     * @throws NullPointerException if {@code slots} or any stack in it is null
     */
    ItemStack[] addItem(ItemStack... slots);

    /**
     * Returns {@code true} if this container has enough space for the item, including partial stacks
     * that can stack with it.
     *
     * @param item the item to test
     * @return {@code true} if the item can fit
     * @throws NullPointerException if {@code item} is null
     */
    boolean canAddItem(ItemStack item);

    /**
     * Removes the requested amounts from stacks that can stack with each requested item.
     *
     * <p>The operation is best-effort. It may remove some items even when the full requested amount is not
     * available. Returned leftovers keep the remaining requested counts.</p>
     *
     * @param slots the item amounts to remove
     * @return any requested stack remainders that could not be removed, or an empty array if all requested
     * items were removed
     * @throws NullPointerException if {@code slots} or any stack in it is null
     */
    ItemStack[] removeItem(ItemStack... slots);

    /**
     * Returns a snapshot of all slots.
     *
     * @return an array with {@link #size()} entries; empty slots are {@link ItemStack#EMPTY}
     */
    ItemStack[] getContents();

    /**
     * Replaces this container's contents.
     *
     * <p>If the array is shorter than {@link #size()}, remaining slots are cleared. Null entries are treated
     * as {@link ItemStack#EMPTY}.</p>
     *
     * @param items replacement contents
     * @throws IllegalArgumentException if the array is larger than this container
     */
    void setContents(ItemStack[] items);

    /**
     * Notifies listeners of the current contents without mutating slots.
     */
    void refresh();

    /**
     * Returns {@code true} if this container contains at least the requested amount across stacks that can
     * stack with the item.
     *
     * @param item the item and amount to search for
     * @return {@code true} if enough stackable items are present
     * @throws NullPointerException if {@code item} is null
     */
    boolean contains(ItemStack item);

    /**
     * Returns all slots containing stacks that can stack with the given item.
     *
     * @param item the item to search for
     * @return matching slots keyed by slot index
     * @throws NullPointerException if {@code item} is null
     */
    Map<Integer, ItemStack> all(ItemStack item);

    /**
     * Returns the first slot containing a stackable item with at least the requested count.
     *
     * @param item the item to search for
     * @return the matching slot index, or {@code -1} when no slot matches
     * @throws NullPointerException if {@code item} is null
     */
    default int first(ItemStack item) {
        return first(item, false);
    }

    /**
     * Searches for the first occurrence of a stackable target item.
     *
     * @param item  target item
     * @param exact if true the item count must match exactly; otherwise a slot with at least the requested
     *              count is accepted
     * @return the first matching slot index, or {@code -1} when no slot matches
     * @throws NullPointerException if {@code item} is null
     */
    int first(ItemStack item, boolean exact);

    /**
     * Returns the first slot where the full item count can fit.
     *
     * @param item the item to search space for
     * @return the matching slot index, or {@code -1} when no slot can fit the item
     * @throws NullPointerException if {@code item} is null
     */
    default int firstFit(ItemStack item) {
        return firstFit(item, false);
    }

    /**
     * Returns the first slot where the item can fit, either by stacking with the existing item or by
     * using an empty slot.
     *
     * @param item   item to search space for
     * @param single if {@code true}, tests whether one item can fit instead of the stack's full count
     * @return the first slot index that can fit the item, or {@code -1} when no slot can fit it
     * @throws NullPointerException if {@code item} is null
     */
    int firstFit(ItemStack item, boolean single);

    /**
     * Searches for the first empty slot.
     *
     * @return the first empty slot index, or {@code -1} when no slots are empty
     */
    int firstEmpty();

    /**
     * Searches for the first slot containing an item.
     *
     * @return the first non-empty slot index, or {@code -1} when all slots are empty
     */
    int firstNonEmpty();

    /**
     * Returns how much space remains for the target item, including matching partial stacks.
     *
     * @param item target item
     * @return the number of additional items that can fit
     * @throws NullPointerException if {@code item} is null
     */
    int getFreeSpace(ItemStack item);

    /**
     * Decreases the item count in the given slot by one.
     *
     * <p>If the count reaches zero, the slot becomes {@link ItemStack#EMPTY}.</p>
     *
     * @param slot target slot index
     * @throws IndexOutOfBoundsException if the slot is outside this container's slot range
     */
    void decrementCount(int slot);

    /**
     * Increases the item count in the given slot by one.
     *
     * <p>This method does not enforce the container or item stack size cap.</p>
     *
     * @param slot target slot index
     * @throws IndexOutOfBoundsException if the slot is outside this container's slot range
     */
    void incrementCount(int slot);

    /**
     * Removes every stack that can stack with the given item.
     *
     * @param item the item to remove
     * @throws NullPointerException if {@code item} is null
     */
    void remove(ItemStack item);

    /**
     * Clears every slot in this container.
     */
    void clear();

    /**
     * Returns whether every slot is occupied and every stack is at its effective stack limit.
     *
     * @return {@code true} if no slot can accept more items
     */
    boolean isFull();

    /**
     * Returns whether every slot is empty.
     *
     * @return {@code true} if this container has no items
     */
    boolean isEmpty();

    /**
     * Visits each slot in index order.
     *
     * @param consumer receives the item and zero-based slot index
     * @throws NullPointerException if {@code consumer} is null
     */
    void forEachSlot(ObjIntConsumer<ItemStack> consumer);
}
