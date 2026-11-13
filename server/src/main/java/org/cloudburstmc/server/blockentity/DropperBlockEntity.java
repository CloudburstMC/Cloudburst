package org.cloudburstmc.server.blockentity;

import org.cloudburstmc.api.blockentity.BlockEntityType;
import org.cloudburstmc.api.blockentity.Dropper;
import org.cloudburstmc.api.inventory.ItemContainer;
import org.cloudburstmc.api.inventory.view.BlockDropperView;
import org.cloudburstmc.api.inventory.view.SlotGroup;
import org.cloudburstmc.api.inventory.view.SlotGroupType;
import org.cloudburstmc.api.inventory.view.SlotGroupTypes;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.level.chunk.Chunk;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.blockentity.data.CloudDropperData;
import org.cloudburstmc.server.container.ContainerListener;

import java.util.HashSet;

/**
 * Block entity implementation for a dropper: a 9-slot container that ejects one item into the world
 * or into an adjacent container when triggered by redstone.
 */
public class DropperBlockEntity extends ContainerBlockEntity implements Dropper, BlockDropperView {

    private final CloudDropperData persistenceState;

    public DropperBlockEntity(BlockEntityType<?> type, Chunk chunk, Vector3i position) {
        CloudDropperData state = new CloudDropperData();
        super(type, chunk, position, state);
        this.persistenceState = state;
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
    protected CloudDropperData data() {
        return this.persistenceState;
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
        if (!this.isClosed()) {
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
