package org.cloudburstmc.server.blockentity;

import org.cloudburstmc.api.blockentity.Barrel;
import org.cloudburstmc.api.blockentity.BlockEntityType;
import org.cloudburstmc.api.blockentity.data.BarrelData;
import org.cloudburstmc.api.blockentity.data.BlockEntityData;
import org.cloudburstmc.api.blockentity.data.ContainerData;
import org.cloudburstmc.api.blockentity.data.SlottedItem;
import org.cloudburstmc.server.container.ContainerListener;
import org.cloudburstmc.api.inventory.view.BlockStorageView;
import org.cloudburstmc.api.inventory.view.SlotGroup;
import org.cloudburstmc.api.inventory.view.SlotGroupType;
import org.cloudburstmc.api.inventory.view.SlotGroupTypes;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.level.chunk.Chunk;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.container.CloudContainer;

import java.util.HashSet;

/**
 * Block entity implementation for a barrel: a 27-slot storage container.
 */
public class BarrelBlockEntity extends ContainerBlockEntity implements Barrel, BlockStorageView {

    private String lootTable;
    private Integer lootTableSeed;

    public BarrelBlockEntity(BlockEntityType<?> type, Chunk chunk, Vector3i position) {
        super(type, chunk, position, new CloudContainer(27));
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
    protected void applyData(BlockEntityData data) {
        super.applyData(data);
        BarrelData barrel = (BarrelData) data;
        for (SlottedItem entry : barrel.getItems()) {
            this.container.setItem(entry.slot(), entry.item());
        }
        this.lootTable = barrel.getLootTable();
        this.lootTableSeed = barrel.getLootTableSeed();
    }

    @Override
    protected void fillData(BlockEntityData data) {
        super.fillData(data);
        BarrelData barrel = (BarrelData) data;
        barrel.setItems(ContainerData.slotted(this.container.getContents()));
        barrel.setLootTable(this.lootTable);
        barrel.setLootTableSeed(this.lootTableSeed);
    }

    public String getLootTable() {
        return lootTable;
    }

    public void setLootTable(String lootTable) {
        this.lootTable = lootTable;
    }

    public Integer getLootTableSeed() {
        return lootTableSeed;
    }

    public void setLootTableSeed(Integer lootTableSeed) {
        this.lootTableSeed = lootTableSeed;
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
        this.container.clear(); // Stop items from being moved around by another player in the inventory
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
}
