package org.cloudburstmc.server.container.view;

import org.cloudburstmc.api.container.ContainerViewType;
import org.cloudburstmc.api.container.view.ContainerView;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.server.container.CloudContainer;

public abstract class CloudContainerView implements ContainerView {

    protected final ContainerViewType<?> type;
    protected final CloudContainer container;
    protected final int size;
    protected final int offset;

    protected CloudContainerView(ContainerViewType<?> type, CloudContainer container) {
        this(type, container, container.size(), 0);
    }

    protected CloudContainerView(ContainerViewType<?> type, CloudContainer container, int size, int offset) {
        if (!type.getContainerViewClass().isInstance(this)) {
            throw new IllegalArgumentException("Invalid container view type");
        }
        this.type = type;
        this.container = container;
        this.size = size;
        this.offset = offset;
    }

    @Override
    public ContainerViewType<?> getViewType() {
        return type;
    }

    @Override
    public CloudContainer getContainer() {
        return container;
    }

    @Override
    public ItemStack getItem(int slot) {
        return container.getItem(slot + offset);
    }

    @Override
    public void setItem(int slot, ItemStack itemStack) {
        container.setItem(slot + offset, itemStack);
    }

    @Override
    public int size() {
        return size;
    }
}
