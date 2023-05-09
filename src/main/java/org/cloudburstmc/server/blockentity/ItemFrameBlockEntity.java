package org.cloudburstmc.server.blockentity;

import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.blockentity.BlockEntityType;
import org.cloudburstmc.api.blockentity.ItemFrame;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.level.chunk.Chunk;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.server.item.ItemUtils;

import java.util.Objects;

/**
 * Created by Pub4Game on 03.07.2016.
 */
public class ItemFrameBlockEntity extends BaseBlockEntity implements ItemFrame {

    private ItemStack item;
    private float itemRotation;
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
        tag.listenForFloat("ItemRotation", value -> this.itemRotation = value);
        tag.listenForFloat("ItemDropChance", value -> this.itemDropChance = value);
    }

    @Override
    protected void saveClientData(NbtMapBuilder tag) {
        super.saveClientData(tag);

        if (!ItemUtils.isNull(this.item)) {
            tag.putCompound("Item", ItemUtils.serializeItem(this.item));
            tag.putFloat("ItemRotation", this.itemRotation);
            tag.putFloat("ItemDropChance", this.itemDropChance);
        }
    }

    @Override
    public boolean isValid() {
        return this.getBlockState().getType() == BlockTypes.FRAME;
    }

    @Override
    public int getItemRotation() {
        return (int) this.itemRotation;
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
        return this.item;
    }

    @Override
    public void setItem(ItemStack item) {
        if (!Objects.equals(this.item, item)) {
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
        return this.getItem() == null || this.getItem() == ItemStack.EMPTY ? 0 : this.getItemRotation() % 8 + 1;
    }

    @Override
    public boolean isSpawnable() {
        return true;
    }
}
