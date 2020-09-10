package org.cloudburstmc.server.blockentity.impl;

import com.nukkitx.math.vector.Vector3i;
import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtMapBuilder;
import com.nukkitx.nbt.NbtType;
import org.cloudburstmc.server.block.BlockIds;
import org.cloudburstmc.server.blockentity.Barrel;
import org.cloudburstmc.server.blockentity.BlockEntityType;
import org.cloudburstmc.server.inventory.BarrelInventory;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.item.ItemUtils;
import org.cloudburstmc.server.level.chunk.Chunk;
import org.cloudburstmc.server.player.Player;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class BarrelBlockEntity extends BaseBlockEntity implements Barrel {

    private final BarrelInventory inventory = new BarrelInventory(this);

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
        for (Map.Entry<Integer, ItemStack> entry : this.inventory.getContents().entrySet()) {
            items.add(ItemUtils.serializeItem(entry.getValue(), entry.getKey()));
        }
        tag.putList("Items", NbtType.COMPOUND, items);
    }

    @Override
    public void close() {
        if (!closed) {
            for (Player player : new HashSet<>(this.getInventory().getViewers())) {
                player.removeWindow(this.getInventory());
            }
            super.close();
        }
    }

    @Override
    public void onBreak() {
        for (ItemStack content : this.inventory.getContents().values()) {
            this.getLevel().dropItem(this.getPosition(), content);
        }
        this.inventory.clearAll(); // Stop items from being moved around by another player in the inventory
    }


    @Override
    public boolean isValid() {
        return getBlockState().getType() == BlockIds.BARREL;
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