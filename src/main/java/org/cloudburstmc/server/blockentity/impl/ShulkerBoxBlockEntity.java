package org.cloudburstmc.server.blockentity.impl;

import com.nukkitx.math.vector.Vector3i;
import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtMapBuilder;
import com.nukkitx.nbt.NbtType;
import org.cloudburstmc.server.block.BlockType;
import org.cloudburstmc.server.block.BlockTypes;
import org.cloudburstmc.server.blockentity.BlockEntityType;
import org.cloudburstmc.server.blockentity.ShulkerBox;
import org.cloudburstmc.server.inventory.ShulkerBoxInventory;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.item.ItemUtils;
import org.cloudburstmc.server.level.chunk.Chunk;
import org.cloudburstmc.server.player.Player;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * Created by PetteriM1
 */
public class ShulkerBoxBlockEntity extends BaseBlockEntity implements ShulkerBox {

    private final ShulkerBoxInventory inventory = new ShulkerBoxInventory(this);
    private byte facing;

    public ShulkerBoxBlockEntity(BlockEntityType<?> type, Chunk chunk, Vector3i position) {
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
        this.facing = tag.getByte("facing");
    }

    @Override
    public void saveAdditionalData(NbtMapBuilder tag) {
        super.saveAdditionalData(tag);

        List<NbtMap> items = new ArrayList<>();
        for (Map.Entry<Integer, ItemStack> entry : this.inventory.getContents().entrySet()) {
            items.add(ItemUtils.serializeItem(entry.getValue(), entry.getKey()));
        }
        tag.putList("Items", NbtType.COMPOUND, items);
        tag.putByte("facing", this.facing);
    }

    public byte getFacing() {
        return facing;
    }

    public void setFacing(int facing) {
        this.facing = (byte) facing;
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
    public boolean isValid() {
        BlockType type = this.getBlockState().getType();
        return type == BlockTypes.SHULKER_BOX || type == BlockTypes.UNDYED_SHULKER_BOX;
    }

    @Override
    public ShulkerBoxInventory getInventory() {
        return inventory;
    }

    @Override
    public boolean isSpawnable() {
        return true;
    }
}
