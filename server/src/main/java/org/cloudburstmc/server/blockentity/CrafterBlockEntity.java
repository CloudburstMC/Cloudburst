package org.cloudburstmc.server.blockentity;

import org.cloudburstmc.api.block.BlockTraits;
import org.cloudburstmc.api.blockentity.BlockEntityType;
import org.cloudburstmc.api.blockentity.Crafter;
import org.cloudburstmc.api.blockentity.data.BlockEntityData;
import org.cloudburstmc.api.blockentity.data.CrafterData;
import org.cloudburstmc.api.inventory.ItemContainer;
import org.cloudburstmc.server.container.ContainerListener;
import org.cloudburstmc.api.inventory.view.CrafterView;
import org.cloudburstmc.api.inventory.view.SlotGroup;
import org.cloudburstmc.api.inventory.view.SlotGroupType;
import org.cloudburstmc.api.inventory.view.SlotGroupTypes;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.level.chunk.Chunk;
import org.cloudburstmc.math.vector.Vector3i;

import java.util.HashSet;

/**
 * Block entity implementation for a crafter: a 9-slot container that supports per-slot disable flags
 * for use with automated crafting when powered by redstone.
 */
public class CrafterBlockEntity extends ContainerBlockEntity implements Crafter, CrafterView {

    public CrafterBlockEntity(BlockEntityType<?> type, Chunk chunk, Vector3i position) {
        super(type, chunk, position);
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
    protected CrafterData data() {
        return (CrafterData) super.data();
    }

    @Override
    protected ItemContainer items() {
        return data().getItems();
    }

    @Override
    public void close() {
        if (!closed) {
            for (ContainerListener listener : new HashSet<>(this.getContainer().getListeners())) {
                listener.onInventoryRemoved(this.getContainer());
            }
            super.close();
        }
    }

    @Override
    public void onBreak() {
        for (ItemStack content : this.getContainer().getContents()) {
            this.dropItemOnBreak(content);
        }
        this.getContainer().clear();
    }

    @Override
    public boolean isSpawnable() {
        return true;
    }

    @Override
    public boolean isSlotDisabled(int slot) {
        checkSlot(slot);
        return (data().getDisabledSlots() & (1 << slot)) != 0;
    }

    @Override
    public void setSlotDisabled(int slot, boolean disabled) {
        checkSlot(slot);
        short disabledSlots = data().getDisabledSlots();
        if (disabled) {
            disabledSlots |= (short) (1 << slot);
        } else {
            disabledSlots &= (short) ~(1 << slot);
        }
        data().setDisabledSlots(disabledSlots);
    }

    @Override
    public boolean isPowered() {
        return getBlockState().ensureTrait(BlockTraits.CRAFTER_TRIGGERED);
    }
}
