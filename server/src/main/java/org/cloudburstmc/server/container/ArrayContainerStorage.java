package org.cloudburstmc.server.container;

import org.cloudburstmc.api.item.ItemStack;

import java.util.Arrays;
import java.util.Objects;

public class ArrayContainerStorage implements ContainerStorage {

    ItemStack[] slots;

    public ArrayContainerStorage(int size) {
        this.slots = new ItemStack[size];
        Arrays.fill(this.slots, ItemStack.EMPTY);
    }

    @Override
    public ItemStack get(int slot) {
        return this.slots[slot];
    }

    @Override
    public void set(int slot, ItemStack itemStack) {
        this.slots[slot] = itemStack;
    }

    @Override
    public int size() {
        return this.slots.length;
    }

    @Override
    public void resize(int size) {
        int oldSize = this.slots.length;
        this.slots = Arrays.copyOf(this.slots, size);
        if (size > oldSize) {
            Arrays.fill(this.slots, oldSize, size, ItemStack.EMPTY);
        }
    }

    @Override
    public ItemStack[] getContents() {
        return Arrays.copyOf(this.slots, this.slots.length);
    }

    @Override
    public void setContents(ItemStack[] contents) {
        if (contents.length > this.slots.length) {
            throw new IllegalArgumentException("Contents length is greater than the storage size");
        }
        for (int i = 0; i < contents.length; ++i) {
            Objects.requireNonNull(contents[i], "contents[" + i + "]");
        }
        // Copy the array contents
        int oldSize = contents.length;
        contents = Arrays.copyOf(contents, this.slots.length);
        if (oldSize < this.slots.length) {
            // Fill the rest with empty stacks
            Arrays.fill(contents, oldSize, this.slots.length, ItemStack.EMPTY);
        }
    }
}
