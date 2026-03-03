package org.cloudburstmc.api.inventory.view;

import org.cloudburstmc.api.item.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Represents a named group of item slots within an open inventory screen.
 *
 * <p>A slot group corresponds directly to a {@code ContainerSlotType} bucket —
 * for example the furnace ingredient slot, the player's 36-slot main inventory, or an armor
 * equipment section. Multiple slot groups are combined inside a single
 * {@link org.cloudburstmc.api.inventory.InventoryScreen} to form the complete window shown
 * to the player.</p>
 *
 * <p>Typed sub-interfaces such as {@link FurnaceView}, {@link StorageView}, and {@link ArmorView}
 * add named accessors for the slots specific to each container type. Most plugin code should
 * interact with those typed interfaces rather than this base.</p>
 */
public interface SlotGroup {

    /**
     * Returns the type token identifying what kind of slot group this is.
     *
     * @return the slot group type
     */
    SlotGroupType<? extends SlotGroup> getSlotGroupType();

    /**
     * Returns the item at the given slot index within this group.
     *
     * @param slot the slot index (0-based, within this group)
     * @return the item stack, never {@code null}
     */
    ItemStack getItem(int slot);

    /**
     * Sets the item at the given slot index within this group.
     *
     * @param slot      the slot index (0-based, within this group)
     * @param itemStack the item to place; use {@link ItemStack#EMPTY} to clear
     */
    void setItem(int slot, ItemStack itemStack);

    /**
     * Returns the number of slots in this group.
     *
     * @return the slot count
     */
    int size();

    /**
     * Returns the maximum number of items that can stack in the given slot.
     *
     * <p>The default implementation returns {@code 64}, which is correct for most slots.
     * Implementations should override this when a slot has a different limit — for example,
     * a slot that only accepts tools (max-stack 1) or a slot whose limit is item-dependent.
     * When the limit depends on the item currently in the slot, implementations should
     * inspect {@link #getItem(int)} to decide.</p>
     *
     * @param slot the slot index (0-based, within this group)
     * @return the maximum stack size for that slot, always &gt;= 1
     */
    default int getMaxStackSize(int slot) {
        return 64;
    }

    /**
     * Returns a snapshot of all items in this group as an array.
     *
     * <p>The returned array has length {@link #size()}, where index {@code i}
     * corresponds to {@link #getItem(int) getItem(i)}. Modifying the returned
     * array does not affect the underlying slot group.</p>
     *
     * @return a new array containing all items in this group
     */
    default ItemStack[] getContents() {
        ItemStack[] contents = new ItemStack[size()];
        for (int i = 0; i < contents.length; i++) {
            contents[i] = getItem(i);
        }
        return contents;
    }

    /**
     * Replaces the contents of this slot group with the given array.
     *
     * <p>The array must have a length equal to {@link #size()}. Each element replaces
     * the corresponding slot; a {@code null} or {@link ItemStack#EMPTY} element clears
     * that slot.</p>
     *
     * @param contents the new contents; length must equal {@link #size()}
     * @throws IllegalArgumentException if {@code contents.length != size()}
     */
    default void setContents(ItemStack[] contents) {
        if (contents.length != size()) {
            throw new IllegalArgumentException(
                    "contents length " + contents.length + " does not match slot group size " + size());
        }
        for (int i = 0; i < contents.length; i++) {
            setItem(i, contents[i] != null ? contents[i] : ItemStack.EMPTY);
        }
    }

    /**
     * Returns the index of the first empty slot in this group, or {@code -1} if all slots are occupied.
     *
     * @return the first empty slot index, or {@code -1}
     */
    default int firstEmpty() {
        for (int i = 0; i < size(); i++) {
            if (getItem(i).isEmpty()) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Returns {@code true} if this slot group contains at least one stack that matches
     * the given item (same type and metadata, regardless of count).
     *
     * @param item the item to search for
     * @return {@code true} if a matching stack is present
     */
    default boolean contains(ItemStack item) {
        for (int i = 0; i < size(); i++) {
            if (getItem(i).isSimilarMetadata(item)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns {@code true} if this slot group contains at least {@code amount} items in total
     * across all stacks that match the given item (same type and metadata).
     *
     * @param item   the item to search for
     * @param amount the minimum total count required
     * @return {@code true} if the combined matching count is &gt;= {@code amount}
     */
    default boolean contains(ItemStack item, int amount) {
        int found = 0;
        for (int i = 0; i < size(); i++) {
            ItemStack slot = getItem(i);
            if (slot.isSimilarMetadata(item)) {
                found += slot.getCount();
                if (found >= amount) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns the index of the first slot whose item matches the given item
     * (same type and metadata, regardless of count), or {@code -1} if none.
     *
     * @param item the item to search for
     * @return the first matching slot index, or {@code -1}
     */
    default int first(ItemStack item) {
        for (int i = 0; i < size(); i++) {
            if (getItem(i).isSimilarMetadata(item)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Returns the index of the first slot whose item matches the given item
     * (same type and metadata) <em>and</em> whose count is at least {@code amount},
     * or {@code -1} if no such slot exists.
     *
     * @param item   the item to search for
     * @param amount the minimum stack count required in the slot
     * @return the first matching slot index, or {@code -1}
     */
    default int first(ItemStack item, int amount) {
        for (int i = 0; i < size(); i++) {
            ItemStack slot = getItem(i);
            if (slot.isSimilarMetadata(item) && slot.getCount() >= amount) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Removes all stacks in this slot group that match the given item
     * (same type and metadata, regardless of count).
     *
     * @param item the item to remove
     */
    default void remove(ItemStack item) {
        for (int i = 0; i < size(); i++) {
            if (getItem(i).isSimilarMetadata(item)) {
                setItem(i, ItemStack.EMPTY);
            }
        }
    }

    /**
     * Clears all slots in this group, setting every slot to {@link ItemStack#EMPTY}.
     */
    default void clear() {
        for (int i = 0; i < size(); i++) {
            setItem(i, ItemStack.EMPTY);
        }
    }

    /**
     * Attempts to add the given item stacks to this slot group, first filling existing partial
     * stacks of matching type and metadata, then occupying empty slots.
     *
     * <p>Per-slot stack limits are respected by calling {@link #getMaxStackSize(int)} for each
     * slot. Implementations that host items with special stack limits (e.g. tools with a max of 1)
     * should override {@code getMaxStackSize} accordingly.</p>
     *
     * @param items the item stacks to add; {@link ItemStack#EMPTY} entries are ignored
     * @return an array of any items that could not fit (leftovers); empty if all items were added
     */
    default ItemStack[] addItem(ItemStack... items) {
        List<ItemStack> remaining = new ArrayList<>(Arrays.asList(items));
        remaining.removeIf(s -> s.isEmpty() || s.getCount() <= 0);

        for (int i = 0; i < size() && !remaining.isEmpty(); i++) {
            ItemStack slot = getItem(i);
            int maxStack = getMaxStackSize(i);
            if (slot.isEmpty() || slot.getCount() >= maxStack) {
                continue;
            }
            for (int j = 0; j < remaining.size(); j++) {
                ItemStack toAdd = remaining.get(j);
                if (!slot.isSimilarMetadata(toAdd)) {
                    continue;
                }
                int space = maxStack - slot.getCount();
                int transfer = Math.min(space, toAdd.getCount());
                setItem(i, slot.increaseCount(transfer));
                slot = getItem(i);
                toAdd = toAdd.decreaseCount(transfer);
                if (toAdd.getCount() <= 0) {
                    remaining.remove(j);
                    j--;
                } else {
                    remaining.set(j, toAdd);
                }
                if (slot.getCount() >= maxStack) {
                    break;
                }
            }
        }

        for (int i = 0; i < size() && !remaining.isEmpty(); i++) {
            if (!getItem(i).isEmpty()) {
                continue;
            }
            ItemStack toAdd = remaining.get(0);
            int amount = Math.min(getMaxStackSize(i), toAdd.getCount());
            setItem(i, toAdd.withCount(amount));
            toAdd = toAdd.decreaseCount(amount);
            if (toAdd.getCount() <= 0) {
                remaining.remove(0);
            } else {
                remaining.set(0, toAdd);
            }
        }

        return remaining.toArray(new ItemStack[0]);
    }
}
