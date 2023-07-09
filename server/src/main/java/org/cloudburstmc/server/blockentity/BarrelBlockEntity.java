package org.cloudburstmc.server.blockentity;

import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.blockentity.Barrel;
import org.cloudburstmc.api.blockentity.BlockEntityType;
import org.cloudburstmc.api.inventory.BarrelInventory;
import org.cloudburstmc.api.inventory.InventoryListener;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.level.chunk.Chunk;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.server.inventory.CloudBarrelInventory;
import org.cloudburstmc.server.item.ItemUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class BarrelBlockEntity extends BaseBlockEntity implements Barrel {

    private final BarrelInventory inventory = new CloudBarrelInventory(this);

    public BarrelBlockEntity(BlockEntityType<?> type, Chunk chunk, Vector3i position) {
        super(type, chunk, position);
    }

    @Override
    public void loadAdditionalData(NbtMap tag) {
        super.loadAdditionalData(tag);

        tag.listenForList("Items", NbtType.COMPOUND, tags -> {
            for (NbtMap itemTag : tags) {
                ItemStack item = ItemUtils.deserializeItem(itemTag);
                this.inventory.setItem(itemTag.getByte("Slot"), item);
            }
        });
    }

    @Override
    public void saveAdditionalData(NbtMapBuilder tag) {
        super.saveAdditionalData(tag);

        List<NbtMap> items = new ArrayList<>();
        ItemStack[] contents = this.inventory.getContents();
        for (int i = 0; i < this.inventory.getSize(); i++) {
            items.add(ItemUtils.serializeItem(contents[i], i));
        }
        tag.putList("Items", NbtType.COMPOUND, items);
    }

    @Override
    public void close() {
        if (!closed) {
            for (InventoryListener listener : new HashSet<>(this.getInventory().getListeners())) {
                listener.onInventoryRemoved(this.getInventory());
            }
            super.close();
        }
    }

    @Override
    public void onBreak() {
        for (ItemStack content : this.inventory.getContents()) {
            this.getLevel().dropItem(this.getPosition(), content);
        }
        this.inventory.clearAll(); // Stop items from being moved around by another player in the inventory
    }


    @Override
    public boolean isValid() {
        return getBlockState().getType() == BlockTypes.BARREL;
    }

    @Override
    public BarrelInventory getInventory() {
        return this.inventory;
    }

    @Override
    public boolean isSpawnable() {
        return true;
    }
}
