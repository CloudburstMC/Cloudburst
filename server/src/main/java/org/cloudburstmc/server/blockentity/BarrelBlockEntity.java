package org.cloudburstmc.server.blockentity;

import org.cloudburstmc.api.blockentity.Barrel;
import org.cloudburstmc.api.blockentity.BlockEntityType;
import org.cloudburstmc.api.blockentity.data.BarrelData;
import org.cloudburstmc.api.blockentity.data.BlockEntityData;
import org.cloudburstmc.api.inventory.ItemContainer;
import org.cloudburstmc.server.container.ContainerListener;
import org.cloudburstmc.api.inventory.view.BlockStorageView;
import org.cloudburstmc.api.inventory.view.SlotGroup;
import org.cloudburstmc.api.inventory.view.SlotGroupType;
import org.cloudburstmc.api.inventory.view.SlotGroupTypes;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.level.chunk.Chunk;
import org.cloudburstmc.math.vector.Vector3i;

import java.util.HashSet;

/**
 * Block entity implementation for a barrel: a 27-slot storage container.
 */
public class BarrelBlockEntity extends ContainerBlockEntity implements Barrel, BlockStorageView {

    public BarrelBlockEntity(BlockEntityType<?> type, Chunk chunk, Vector3i position) {
        super(type, chunk, position);
    }

    @Override
    public SlotGroupType<? extends SlotGroup> getSlotGroupType() {
        return SlotGroupTypes.BARREL;
    }

    @Override
    protected Class<? extends BlockEntityData> minimumDataClass() {
        return BarrelData.class;
    }

    @Override
    protected BarrelData data() {
        return (BarrelData) super.data();
    }

    @Override
    protected ItemContainer items() {
        return data().getItems();
    }

    public String getLootTable() {
        return data().getLootTable();
    }

    public void setLootTable(String lootTable) {
        data().setLootTable(lootTable);
    }

    public Integer getLootTableSeed() {
        return data().getLootTableSeed();
    }

    public void setLootTableSeed(Integer lootTableSeed) {
        data().setLootTableSeed(lootTableSeed);
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
        this.getContainer().clear(); // Stop items from being moved around by another player in the inventory
    }

    @Override
    public boolean isSpawnable() {
        return true;
    }
}
