package org.cloudburstmc.server.container;

import org.cloudburstmc.api.item.ItemStack;

import java.util.Arrays;
import java.util.Objects;

public class MultiContainerStorage implements ContainerStorage {

    private final ContainerStorage[] children;
    private final int size;

    public MultiContainerStorage(ContainerStorage... children) {
        int size = -1;
        for (ContainerStorage child : children) {
            Objects.requireNonNull(child, "child");
            if (size != -1 && size != child.size()) {
                throw new IllegalArgumentException("Children must have the same size");
            }
            size = child.size();
        }
        if (size == -1) {
            throw new IllegalArgumentException("No children");
        }
        this.children = Arrays.copyOf(children, children.length);
        this.size = size;
    }

    @Override
    public ItemStack get(int slot) {
        return this.children[slot / size].get(slot % size);
    }

    @Override
    public void set(int slot, ItemStack itemStack) {
        this.children[slot / size].set(slot % size, itemStack);
    }

    @Override
    public int size() {
        return this.size * this.children.length;
    }

    @Override
    public void resize(int size) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ItemStack[] getContents() {
        ItemStack[] contents = new ItemStack[this.size()];

        for (int i = 0; i < this.size(); ++i) {
            contents[i] = this.get(i);
        }

        return contents;
    }

    @Override
    public void setContents(ItemStack[] contents) {
        for (int i = 0; i < this.children.length; ++i) {
            this.children[i].setContents(Arrays.copyOfRange(contents, i * this.size, (i + 1) * this.size));
        }
    }
}
