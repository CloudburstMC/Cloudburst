package org.cloudburstmc.server.blockentity;

import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.blockentity.Barrel;
import org.cloudburstmc.api.blockentity.BlockEntityType;
import org.cloudburstmc.api.container.ContainerListener;
import org.cloudburstmc.api.container.ContainerViewTypes;
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

public class BarrelBlockEntity extends ContainerBlockEntity implements Barrel {

    private final CloudContainer container = new CloudContainer(27);

    public BarrelBlockEntity(BlockEntityType<?> type, Chunk chunk, Vector3i position) {
        super(type, chunk, position, new CloudContainer(27), ContainerViewTypes.BARREL);
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
        this.container.clear(); // Stop items from being moved around by another player in the inventory
    }


    @Override
    public boolean isValid() {
        return getBlockState().getType() == BlockTypes.BARREL;
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
