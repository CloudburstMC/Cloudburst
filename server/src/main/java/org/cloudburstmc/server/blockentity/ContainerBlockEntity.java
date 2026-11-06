package org.cloudburstmc.server.blockentity;

import org.cloudburstmc.api.blockentity.BlockEntity;
import org.cloudburstmc.api.blockentity.BlockEntityType;
import org.cloudburstmc.api.inventory.ItemContainer;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.level.chunk.Chunk;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.container.CloudContainer;

/**
 * Base class for block entities that own a {@link CloudContainer} (item storage).
 *
 * <p>The storage lives in the block entity's data instance; this class only delegates to it.
 * Subclasses return their DTO's container from {@link #items()} and override
 * {@link #getContainer()} only when they expose a different view, like a paired chest's
 * combined container.</p>
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

    public ContainerBlockEntity(BlockEntityType<?> type, Chunk chunk, Vector3i position) {
        super(type, chunk, position);
    }

    /**
     * Returns the single storage container held by this block entity's data instance.
     * Slot accessors use this; {@link #getContainer()} may return a wider view.
     *
     * @return the DTO's container
     */
    protected abstract ItemContainer items();

    public CloudContainer getContainer() {
        return (CloudContainer) items();
    }

    public ItemStack getItem(int slot) {
        return items().getItem(slot);
    }

    public void setItem(int slot, ItemStack itemStack) {
        items().setItem(slot, itemStack);
    }

    public int size() {
        return items().size();
    }

    public BlockEntity getBlockEntity() {
        return this;
    }
}
