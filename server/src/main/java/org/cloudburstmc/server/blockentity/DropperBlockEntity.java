package org.cloudburstmc.server.blockentity;

import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.blockentity.BlockEntityType;
import org.cloudburstmc.api.blockentity.Dropper;
import org.cloudburstmc.server.container.ContainerListener;
import org.cloudburstmc.api.inventory.view.BlockDropperView;
import org.cloudburstmc.api.inventory.view.SlotGroup;
import org.cloudburstmc.api.inventory.view.SlotGroupType;
import org.cloudburstmc.api.inventory.view.SlotGroupTypes;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.level.chunk.Chunk;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.server.container.CloudContainer;
import org.cloudburstmc.server.item.ItemUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Block entity implementation for a dropper: a 9-slot container that ejects one item into the world
 * or into an adjacent container when triggered by redstone.
 */
public class DropperBlockEntity extends ContainerBlockEntity implements Dropper, BlockDropperView {

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
    public void loadAdditionalData(NbtMap tag) {
        super.loadAdditionalData(tag);

        tag.listenForList("Items", NbtType.COMPOUND, tags -> {
            for (NbtMap itemTag : tags) {
                ItemStack item = ItemUtils.deserializeItem(itemTag);
                this.container.setItem(itemTag.getByte("Slot"), item);
            }
        });
    }

    @Override
    public void saveAdditionalData(NbtMapBuilder tag) {
        super.saveAdditionalData(tag);

        List<NbtMap> items = new ArrayList<>();
        ItemStack[] contents = this.container.getContents();
        for (int i = 0; i < this.container.size(); i++) {
            items.add(ItemUtils.serializeItem(contents[i], i));
        }
        tag.putList("Items", NbtType.COMPOUND, items);
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
            this.getLevel().dropItem(this.getPosition(), content);
        }
        this.container.clear();
    }

    @Override
    public boolean isValid() {
        return getBlockState().getType() == BlockTypes.DROPPER;
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
