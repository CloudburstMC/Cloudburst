package org.cloudburstmc.server.blockentity.impl;

import com.nukkitx.math.vector.Vector3i;
import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtMapBuilder;
import org.cloudburstmc.server.block.BlockIds;
import org.cloudburstmc.server.blockentity.BlockEntityType;
import org.cloudburstmc.server.blockentity.ItemFrame;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.item.ItemUtils;
import org.cloudburstmc.server.level.chunk.Chunk;

import java.util.Objects;

import static org.cloudburstmc.server.block.BlockIds.AIR;

/**
 * Created by Pub4Game on 03.07.2016.
 */
public class ItemFrameBlockEntity extends BaseBlockEntity implements ItemFrame {

    private ItemStack item;
    private byte itemRotation;
    private float itemDropChance = 1.0f;

    public ItemFrameBlockEntity(BlockEntityType<?> type, Chunk chunk, Vector3i position) {
        super(type, chunk, position);
    }

    @Override
    public void loadAdditionalData(NbtMap tag) {
        super.loadAdditionalData(tag);

        tag.listenForCompound("Item", itemTag -> {
            this.item = ItemUtils.deserializeItem(itemTag);
        });
        tag.listenForByte("ItemRotation", value -> this.itemRotation = value);
        tag.listenForFloat("ItemDropChance", value -> this.itemDropChance = value);
    }

    @Override
    protected void saveClientData(NbtMapBuilder tag) {
        super.saveClientData(tag);

        if (this.item != null && !this.item.isNull()) {
            tag.putCompound("Item", ItemUtils.serializeItem(this.item));
            tag.putByte("ItemRotation", this.itemRotation);
            tag.putFloat("ItemDropChance", this.itemDropChance);
        }
    }

    @Override
    public boolean isValid() {
        return this.getBlockState().getType() == BlockIds.FRAME;
    }

    @Override
    public int getItemRotation() {
        return this.itemRotation;
    }

    @Override
    public void setItemRotation(int itemRotation) {
        if (this.itemRotation != itemRotation) {
            this.itemRotation = (byte) itemRotation;
            this.setDirty();
            this.getLevel().updateComparatorOutputLevel(this.getPosition());
        }
    }

    @Override
    public ItemStack getItem() {
        return this.item.clone();
    }

    @Override
    public void setItem(ItemStack item) {
        if (!Objects.equals(this.item, item)) {
            this.item = item.clone();
            this.setDirty();

            this.getLevel().updateComparatorOutputLevel(this.getPosition());
        }
    }

    @Override
    public float getItemDropChance() {
        return this.itemDropChance;
    }

    @Override
    public void setItemDropChance(float itemDropChance) {
        if (this.itemDropChance != itemDropChance) {
            this.itemDropChance = itemDropChance;
            this.setDirty();
        }
    }

    public void setDirty() {
        this.spawnToAll();
        super.setDirty();
    }

    @Override
    public int getAnalogOutput() {
        return this.getItem() == null || this.getItem().getId() == AIR ? 0 : this.getItemRotation() % 8 + 1;
    }

    @Override
    public boolean isSpawnable() {
        return true;
    }
}
