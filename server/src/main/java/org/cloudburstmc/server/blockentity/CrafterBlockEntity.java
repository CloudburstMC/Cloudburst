package org.cloudburstmc.server.blockentity;

import org.cloudburstmc.api.block.BlockTraits;
import org.cloudburstmc.api.blockentity.BlockEntityType;
import org.cloudburstmc.api.blockentity.Crafter;
import org.cloudburstmc.api.blockentity.data.BlockEntityData;
import org.cloudburstmc.api.blockentity.data.ContainerData;
import org.cloudburstmc.api.blockentity.data.CrafterData;
import org.cloudburstmc.api.blockentity.data.SlottedItem;
import org.cloudburstmc.server.container.ContainerListener;
import org.cloudburstmc.api.inventory.view.CrafterView;
import org.cloudburstmc.api.inventory.view.SlotGroup;
import org.cloudburstmc.api.inventory.view.SlotGroupType;
import org.cloudburstmc.api.inventory.view.SlotGroupTypes;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.level.chunk.Chunk;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.container.CloudContainer;

import java.util.HashSet;

/**
 * Block entity implementation for a crafter: a 9-slot container that supports per-slot disable flags
 * for use with automated crafting when powered by redstone.
 */
public class CrafterBlockEntity extends ContainerBlockEntity implements Crafter, CrafterView {

    private short disabledSlots = 0;

    public CrafterBlockEntity(BlockEntityType<?> type, Chunk chunk, Vector3i position) {
        super(type, chunk, position, new CloudContainer(9));
    }

    private static void checkSlot(int slot) {
        if (slot < 0 || slot > 8) {
            throw new IndexOutOfBoundsException("Crafter slot must be 0–8, got " + slot);
        }
    }

    @Override
    public SlotGroupType<? extends SlotGroup> getSlotGroupType() {
        return SlotGroupTypes.CRAFTER;
    }

    @Override
    public Crafter getBlockEntity() {
        return this;
    }

    @Override
    protected Class<? extends BlockEntityData> minimumDataClass() {
        return CrafterData.class;
    }

    @Override
    protected void applyData(BlockEntityData data) {
        super.applyData(data);
        CrafterData crafter = (CrafterData) data;
        for (SlottedItem entry : crafter.getItems()) {
            this.container.setItem(entry.slot(), entry.item());
        }
        this.disabledSlots = crafter.getDisabledSlots();
    }

    @Override
    protected void fillData(BlockEntityData data) {
        super.fillData(data);
        CrafterData crafter = (CrafterData) data;
        crafter.setItems(ContainerData.slotted(this.container.getContents()));
        crafter.setDisabledSlots(this.disabledSlots);
    }

    @Override
    public void close() {
        if (!closed) {
            for (ContainerListener listener : new HashSet<>(this.container.getListeners())) {
                listener.onInventoryRemoved(this.container);
            }
            super.close();
        }
    }

    @Override
    public void onBreak() {
        for (ItemStack content : this.container.getContents()) {
            this.dropItemOnBreak(content);
        }
        this.container.clear();
    }

    @Override
    public boolean isSpawnable() {
        return true;
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
        this.container.setItem(slot, itemStack);
    }

    @Override
    public int size() {
        return this.container.size();
    }

    @Override
    public boolean isSlotDisabled(int slot) {
        checkSlot(slot);
        return (this.disabledSlots & (1 << slot)) != 0;
    }

    @Override
    public void setSlotDisabled(int slot, boolean disabled) {
        checkSlot(slot);
        if (disabled) {
            this.disabledSlots |= (short) (1 << slot);
        } else {
            this.disabledSlots &= (short) ~(1 << slot);
        }
    }

    @Override
    public boolean isPowered() {
        return getBlockState().ensureTrait(BlockTraits.CRAFTER_TRIGGERED);
    }
}
