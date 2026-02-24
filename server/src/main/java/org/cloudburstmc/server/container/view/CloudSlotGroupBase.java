package org.cloudburstmc.server.container.view;

import org.cloudburstmc.api.inventory.view.SlotGroup;
import org.cloudburstmc.api.inventory.view.SlotGroupType;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.server.container.CloudContainer;

/**
 * Root abstract base class for all server-side slot group implementations. Stores the
 * {@link SlotGroupType}, backing {@link org.cloudburstmc.server.container.CloudContainer},
 * slot count, and offset, and validates type compatibility at construction time.
 */
public abstract class CloudSlotGroupBase implements SlotGroup {

    protected final SlotGroupType<?> type;
    protected final CloudContainer container;
    protected final int size;
    protected final int offset;

    protected CloudSlotGroupBase(SlotGroupType<?> type, CloudContainer container) {
        this(type, container, container.size(), 0);
    }

    protected CloudSlotGroupBase(SlotGroupType<?> type, CloudContainer container, int size, int offset) {
        if (!type.getSlotGroupClass().isInstance(this)) {
            throw new IllegalArgumentException("Invalid slot group type");
        }
        this.type = type;
        this.container = container;
        this.size = size;
        this.offset = offset;
    }

    @Override
    public SlotGroupType<?> getSlotGroupType() {
        return type;
    }

    /**
     * Returns the backing {@link CloudContainer} for this slot group.
     * <p>
     * This is a server-internal accessor. Plugin code should use the typed slot
     * accessors on the view interfaces instead.
     *
     * @return the underlying container
     */
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
