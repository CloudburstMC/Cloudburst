package org.cloudburstmc.server.blockentity;

import org.cloudburstmc.api.blockentity.BlockEntityType;
import org.cloudburstmc.api.blockentity.Dispenser;
import org.cloudburstmc.api.blockentity.data.BlockEntityData;
import org.cloudburstmc.api.blockentity.data.ContainerData;
import org.cloudburstmc.api.blockentity.data.DispenserData;
import org.cloudburstmc.api.blockentity.data.SlottedItem;
import org.cloudburstmc.server.container.ContainerListener;
import org.cloudburstmc.api.inventory.view.BlockDispenserView;
import org.cloudburstmc.api.inventory.view.SlotGroup;
import org.cloudburstmc.api.inventory.view.SlotGroupType;
import org.cloudburstmc.api.inventory.view.SlotGroupTypes;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.level.chunk.Chunk;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.container.CloudContainer;

import java.util.HashSet;

/**
 * Block entity implementation for a dispenser: a 9-slot container that fires or uses items
 * based on item type when triggered by redstone.
 */
public class DispenserBlockEntity extends ContainerBlockEntity implements Dispenser, BlockDispenserView {

    private String lootTable;
    private Integer lootTableSeed;

    public DispenserBlockEntity(BlockEntityType<?> type, Chunk chunk, Vector3i position) {
        super(type, chunk, position, new CloudContainer(9));
    }

    @Override
    public SlotGroupType<? extends SlotGroup> getSlotGroupType() {
        return SlotGroupTypes.DISPENSER;
    }

    @Override
    public Dispenser getBlockEntity() {
        return this;
    }

    @Override
    protected Class<? extends BlockEntityData> minimumDataClass() {
        return DispenserData.class;
    }

    @Override
    protected void applyData(BlockEntityData data) {
        super.applyData(data);
        DispenserData dispenser = (DispenserData) data;
        for (SlottedItem entry : dispenser.getItems()) {
            this.container.setItem(entry.slot(), entry.item());
        }
        this.lootTable = dispenser.getLootTable();
        this.lootTableSeed = dispenser.getLootTableSeed();
    }

    @Override
    protected void fillData(BlockEntityData data) {
        super.fillData(data);
        DispenserData dispenser = (DispenserData) data;
        dispenser.setItems(ContainerData.slotted(this.container.getContents()));
        dispenser.setLootTable(this.lootTable);
        dispenser.setLootTableSeed(this.lootTableSeed);
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
