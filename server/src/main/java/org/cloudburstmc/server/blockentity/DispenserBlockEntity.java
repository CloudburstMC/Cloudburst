package org.cloudburstmc.server.blockentity;

import org.cloudburstmc.api.blockentity.BlockEntityType;
import org.cloudburstmc.api.blockentity.Dispenser;
import org.cloudburstmc.api.blockentity.data.BlockEntityData;
import org.cloudburstmc.api.blockentity.data.DispenserData;
import org.cloudburstmc.api.inventory.ItemContainer;
import org.cloudburstmc.server.container.ContainerListener;
import org.cloudburstmc.api.inventory.view.BlockDispenserView;
import org.cloudburstmc.api.inventory.view.SlotGroup;
import org.cloudburstmc.api.inventory.view.SlotGroupType;
import org.cloudburstmc.api.inventory.view.SlotGroupTypes;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.level.chunk.Chunk;
import org.cloudburstmc.math.vector.Vector3i;

import java.util.HashSet;

/**
 * Block entity implementation for a dispenser: a 9-slot container that fires or uses items
 * based on item type when triggered by redstone.
 */
public class DispenserBlockEntity extends ContainerBlockEntity implements Dispenser, BlockDispenserView {

    public DispenserBlockEntity(BlockEntityType<?> type, Chunk chunk, Vector3i position) {
        super(type, chunk, position);
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
    protected DispenserData data() {
        return (DispenserData) super.data();
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
        this.getContainer().clear();
    }

    @Override
    public boolean isSpawnable() {
        return true;
    }
}
