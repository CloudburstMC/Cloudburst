package org.cloudburstmc.server.blockentity;

import org.cloudburstmc.api.blockentity.BlockEntityType;
import org.cloudburstmc.api.container.ContainerViewType;
import org.cloudburstmc.api.container.view.ContainerView;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.level.chunk.Chunk;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.container.CloudContainer;

public abstract class ContainerBlockEntity extends BaseBlockEntity implements ContainerView {

    protected final CloudContainer container;
    protected final ContainerViewType<?> viewType;

    public ContainerBlockEntity(BlockEntityType<?> type, Chunk chunk, Vector3i position, CloudContainer container, ContainerViewType<?> viewType) {
        super(type, chunk, position);
        this.container = container;
        this.viewType = viewType;
    }

    @Override
    public CloudContainer getContainer() {
        return container;
    }

    @Override
    public ItemStack getItem(int slot) {
        return container.getItem(slot);
    }

    @Override
    public void setItem(int slot, ItemStack itemStack) {
        container.setItem(slot, itemStack);
    }

    @Override
    public int size() {
        return container.size();
    }

    @Override
    public boolean isValid() {
        return false;
    }

    @Override
    public ContainerViewType<? extends ContainerView> getViewType() {
        return viewType;
    }
}
