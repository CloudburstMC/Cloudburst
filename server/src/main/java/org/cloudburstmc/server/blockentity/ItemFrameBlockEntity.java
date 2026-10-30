package org.cloudburstmc.server.blockentity;

import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockTraits;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.blockentity.BlockEntityType;
import org.cloudburstmc.api.blockentity.ItemFrame;
import org.cloudburstmc.api.blockentity.data.BlockEntityData;
import org.cloudburstmc.api.blockentity.data.ItemFrameData;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.level.chunk.Chunk;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;

import java.util.Objects;
import java.util.Set;

public class ItemFrameBlockEntity extends BaseBlockEntity implements ItemFrame {

    private ItemStack item = ItemStack.EMPTY;
    private int itemRotation;
    private float itemDropChance = 1.0f;
    private boolean fixed;

    public ItemFrameBlockEntity(BlockEntityType<?> type, Chunk chunk, Vector3i position) {
        super(type, chunk, position);
    }

    @Override
    protected void applyData(BlockEntityData data) {
        super.applyData(data);
        ItemFrameData frame = (ItemFrameData) data;
        ItemStack loaded = frame.getItem();
        this.item = loaded != null ? loaded : ItemStack.EMPTY;
        this.fixed = frame.isFixed();
    }

    @Override
    protected void fillData(BlockEntityData data) {
        super.fillData(data);
        ItemFrameData frame = (ItemFrameData) data;
        frame.setItem(this.item.isEmpty() ? null : this.item);
        frame.setFixed(this.fixed);
    }

    @Override
    protected void loadCustom(NbtMap tag) {
        tag.listenForFloat("ItemRotation", value -> {
            if (!Float.isFinite(value) || value % 45 != 0 || value < 0 || value >= 360) {
                throw new IllegalArgumentException("Invalid frame rotation");
            }
            this.itemRotation = (int) (value / 45);
        });

        tag.listenForFloat("ItemDropChance", value -> {
            if (!Float.isFinite(value) || value < 0 || value > 1) {
                throw new IllegalArgumentException("Invalid frame item drop chance");
            }
            this.itemDropChance = value;
        });

        if (!this.item.isEmpty()) {
            this.item = this.item.withCount(1);
        } else {
            this.itemRotation = 0;
        }

        this.updateMapState();
    }

    @Override
    protected void saveCustom(NbtMapBuilder tag) {
        tag.putFloat("ItemRotation", this.itemRotation * 45);
        tag.putFloat("ItemDropChance", this.itemDropChance);
    }

    @Override
    protected void saveCustomClient(NbtMapBuilder tag) {
        tag.putFloat("ItemRotation", this.itemRotation * 45);
        tag.putFloat("ItemDropChance", this.itemDropChance);
    }

    @Override
    protected Set<String> customKeys() {
        return Set.of("ItemRotation", "ItemDropChance");
    }

    @Override
    public void onBreak() {
        this.item = ItemStack.EMPTY;
        this.itemRotation = 0;
    }

    @Override
    public Direction getFacing() {
        return this.getLevel().getBlockState(this.getPosition()).ensureTrait(BlockTraits.FACING_DIRECTION);
    }

    @Override
    public int getItemRotation() {
        return this.itemRotation;
    }

    @Override
    public void setItemRotation(int itemRotation) {
        if (itemRotation < 0 || itemRotation > 7) {
            throw new IllegalArgumentException("Frame rotation must be between zero and seven");
        }

        if (this.itemRotation != itemRotation) {
            this.itemRotation = itemRotation;
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
        Objects.requireNonNull(item, "item");
        ItemStack displayed = item.isEmpty() ? ItemStack.EMPTY : item.withCount(1);

        if (!this.item.equals(displayed)) {
            this.item = displayed;
            if (displayed.isEmpty()) {
                this.itemRotation = 0;
            }

            this.updateMapState();
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
        if (!Float.isFinite(itemDropChance) || itemDropChance < 0 || itemDropChance > 1) {
            throw new IllegalArgumentException("Item drop chance must be between zero and one");
        }

        if (this.itemDropChance != itemDropChance) {
            this.itemDropChance = itemDropChance;
            this.setDirty();
        }
    }

    @Override
    public boolean isFixed() {
        return this.fixed;
    }

    @Override
    public void setFixed(boolean fixed) {
        if (this.fixed != fixed) {
            this.fixed = fixed;
            super.setDirty();
        }
    }

    private void updateMapState() {
        if (this.getLevel() == null) {
            return;
        }
        BlockState state = this.getLevel().getBlockState(this.getPosition());
        if (state.getType() == BlockTypes.FRAME || state.getType() == BlockTypes.GLOW_FRAME) {
            this.getLevel().setBlockState(this.getPosition(), state.withTrait(BlockTraits.HAS_MAP,
                    this.item.getType() == ItemTypes.FILLED_MAP), false, true);
        }
    }

    public void setDirty() {
        this.spawnToAll();
        super.setDirty();
    }

    @Override
    public int getAnalogOutput() {
        return this.getItem().isEmpty() ? 0 : this.getItemRotation() % 8 + 1;
    }

    @Override
    public boolean isSpawnable() {
        return true;
    }
}
