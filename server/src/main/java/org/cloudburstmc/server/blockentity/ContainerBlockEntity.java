package org.cloudburstmc.server.blockentity;

import org.cloudburstmc.api.blockentity.BlockEntity;
import org.cloudburstmc.api.blockentity.BlockEntityType;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.level.chunk.Chunk;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.container.CloudContainer;

/**
 * Base class for block entities that own a {@link CloudContainer} (item storage).
 *
 * <p>This class intentionally does <em>not</em> implement
 * {@link org.cloudburstmc.api.inventory.view.SlotGroup}. The view/slot-group abstraction
 * belongs to the inventory screen layer; block entities are responsible only for storage
 * and NBT serialisation. Concrete subclasses expose a typed view interface
 * (e.g. {@link org.cloudburstmc.api.inventory.view.BlockHopperView}) directly on the
 * block-entity class when required by the hopper/dropper/dispenser/crafter logic that
 * needs to interact with the container outside of a player-opened screen.</p>
 */
public abstract class ContainerBlockEntity extends BaseBlockEntity {

    protected final CloudContainer container;

    public ContainerBlockEntity(BlockEntityType<?> type, Chunk chunk, Vector3i position, CloudContainer container) {
        super(type, chunk, position);
        this.container = container;
    }

    public CloudContainer getContainer() {
        return container;
    }

    public ItemStack getItem(int slot) {
        return container.getItem(slot);
    }

    public void setItem(int slot, ItemStack itemStack) {
        container.setItem(slot, itemStack);
    }

    public int size() {
        return container.size();
    }

    @Override
    public boolean isValid() {
        return false;
    }

    public BlockEntity getBlockEntity() {
        return this;
    }
}
