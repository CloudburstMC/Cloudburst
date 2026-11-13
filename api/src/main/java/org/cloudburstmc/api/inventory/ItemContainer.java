package org.cloudburstmc.api.inventory;

import org.cloudburstmc.api.item.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Item slots addressed by zero-based index. Empty slots return {@link ItemStack#EMPTY}.
 */
public interface ItemContainer {

    /**
     * Returns the number of slots.
     *
     * @return the slot count
     */
    int size();

    /**
     * Returns the item in a slot.
     *
     * @param slot the slot index
     * @return the item stack, or {@link ItemStack#EMPTY} if the slot is empty
     */
    ItemStack getItem(int slot);

    /**
     * Replaces the item in a slot.
     *
     * @param slot      the slot index
     * @param itemStack the replacement, or {@link ItemStack#EMPTY} to clear the slot
     */
    void setItem(int slot, ItemStack itemStack);

    /**
     * Returns the maximum number of items that can stack in the given slot.
     *
     * @param slot the slot index
     * @return the slot's stack limit
     */
    default int getMaxStackSize(int slot) {
        return 64;
    }

    /**
     * Returns a snapshot of all slots in index order, including empty slots.
     *
     * @return a new array containing the slot contents
     */
    default ItemStack[] getContents() {
        ItemStack[] contents = new ItemStack[size()];
        for (int i = 0; i < contents.length; i++) {
            contents[i] = getItem(i);
        }
        return contents;
    }

    /**
     * Replaces all slots in index order. A {@code null} or {@link ItemStack#EMPTY} entry clears its slot.
     *
     * @param contents the new contents
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
     * Returns {@code true} if this slot group contains at least one stack that can stack
     * with the given item, regardless of count.
     *
     * @param item the item to search for
     * @return {@code true} if a matching stack is present
     */
    default boolean contains(ItemStack item) {
        for (int i = 0; i < size(); i++) {
            if (getItem(i).isStackableWith(item)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns {@code true} if this slot group contains at least {@code amount} items in total
     * across all stacks that can stack with the given item.
     *
     * @param item   the item to search for
     * @param amount the minimum total count required
     * @return {@code true} if the combined matching count is &gt;= {@code amount}
     */
    default boolean contains(ItemStack item, int amount) {
        int found = 0;
        for (int i = 0; i < size(); i++) {
            ItemStack slot = getItem(i);
            if (slot.isStackableWith(item)) {
                found += slot.getCount();
                if (found >= amount) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns the index of the first slot whose item can stack with the given item,
     * regardless of count, or {@code -1} if none.
     *
     * @param item the item to search for
     * @return the first matching slot index, or {@code -1}
     */
    default int first(ItemStack item) {
        for (int i = 0; i < size(); i++) {
            if (getItem(i).isStackableWith(item)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Returns the index of the first slot whose item can stack with the given item
     * <em>and</em> whose count is at least {@code amount},
     * or {@code -1} if no such slot exists.
     *
     * @param item   the item to search for
     * @param amount the minimum stack count required in the slot
     * @return the first matching slot index, or {@code -1}
     */
    default int first(ItemStack item, int amount) {
        for (int i = 0; i < size(); i++) {
            ItemStack slot = getItem(i);
            if (slot.isStackableWith(item) && slot.getCount() >= amount) {
                return i;
            }
        }
        return -1;
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
     * Adds items to matching partial stacks before using empty slots, subject to stack limits.
     *
     * @param items the stacks to add, ignoring empty stacks
     * @return the stacks that could not fit
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
                if (!slot.isStackableWith(toAdd)) {
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

            ItemStack toAdd = remaining.getFirst();
            int amount = Math.min(getMaxStackSize(i), toAdd.getCount());
            setItem(i, toAdd.withCount(amount));

            toAdd = toAdd.decreaseCount(amount);
            if (toAdd.getCount() <= 0) {
                remaining.removeFirst();
            } else {
                remaining.set(0, toAdd);
            }
        }

        return remaining.toArray(new ItemStack[0]);
    }

    /**
     * Removes all stacks in this slot group that can stack with the given item.
     *
     * @param item the item to remove
     */
    default void remove(ItemStack item) {
        for (int i = 0; i < size(); i++) {
            if (getItem(i).isStackableWith(item)) {
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
}
