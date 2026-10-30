package org.cloudburstmc.server.blockentity;

import org.cloudburstmc.api.blockentity.BlockEntityType;
import org.cloudburstmc.api.blockentity.Dropper;
import org.cloudburstmc.api.blockentity.data.BlockEntityData;
import org.cloudburstmc.api.blockentity.data.ContainerData;
import org.cloudburstmc.api.blockentity.data.DropperData;
import org.cloudburstmc.api.blockentity.data.SlottedItem;
import org.cloudburstmc.server.container.ContainerListener;
import org.cloudburstmc.api.inventory.view.BlockDropperView;
import org.cloudburstmc.api.inventory.view.SlotGroup;
import org.cloudburstmc.api.inventory.view.SlotGroupType;
import org.cloudburstmc.api.inventory.view.SlotGroupTypes;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.level.chunk.Chunk;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.container.CloudContainer;

import java.util.HashSet;

/**
 * Block entity implementation for a dropper: a 9-slot container that ejects one item into the world
 * or into an adjacent container when triggered by redstone.
 */
public class DropperBlockEntity extends ContainerBlockEntity implements Dropper, BlockDropperView {

    private String lootTable;
    private Integer lootTableSeed;

    public DropperBlockEntity(BlockEntityType<?> type, Chunk chunk, Vector3i position) {
        super(type, chunk, position, new CloudContainer(9));
    }

    @Override
    public SlotGroupType<? extends SlotGroup> getSlotGroupType() {
        return SlotGroupTypes.DROPPER;
    }

    @Override
    public Dropper getBlockEntity() {
        return this;
    }

    @Override
    protected void applyData(BlockEntityData data) {
        super.applyData(data);
        DropperData dropper = (DropperData) data;
        for (SlottedItem entry : dropper.getItems()) {
            this.container.setItem(entry.slot(), entry.item());
        }
        this.lootTable = dropper.getLootTable();
        this.lootTableSeed = dropper.getLootTableSeed();
    }

    @Override
    protected void fillData(BlockEntityData data) {
        super.fillData(data);
        DropperData dropper = (DropperData) data;
        dropper.setItems(ContainerData.slotted(this.container.getContents()));
        dropper.setLootTable(this.lootTable);
        dropper.setLootTableSeed(this.lootTableSeed);
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
        this.container.clear();
    }

    @Override
    public boolean isSpawnable() {
        return true;
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
