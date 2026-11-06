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

    public ItemFrameBlockEntity(BlockEntityType<?> type, Chunk chunk, Vector3i position) {
        super(type, chunk, position);
    }

    @Override
    protected Class<? extends BlockEntityData> minimumDataClass() {
        return ItemFrameData.class;
    }

    @Override
    protected ItemFrameData data() {
        return (ItemFrameData) super.data();
    }

    @Override
    protected void loadCustom(NbtMap tag) {
        tag.listenForFloat("ItemRotation", value -> {
            if (!Float.isFinite(value) || value % 45 != 0 || value < 0 || value >= 360) {
                throw new IllegalArgumentException("Invalid frame rotation");
            }
            data().setItemRotation((int) (value / 45));
        });

        tag.listenForFloat("ItemDropChance", value -> {
            if (!Float.isFinite(value) || value < 0 || value > 1) {
                throw new IllegalArgumentException("Invalid frame item drop chance");
            }
            data().setItemDropChance(value);
        });

        ItemStack item = data().getItem();
        if (item != null && !item.isEmpty()) {
            data().setItem(item.withCount(1));
        } else {
            data().setItem(null);
            data().setItemRotation(0);
        }

        this.updateMapState();
    }

    @Override
    protected void saveCustom(NbtMapBuilder tag) {
        tag.putFloat("ItemRotation", data().getItemRotation() * 45);
        tag.putFloat("ItemDropChance", data().getItemDropChance());
    }

    @Override
    protected void saveCustomClient(NbtMapBuilder tag) {
        tag.putFloat("ItemRotation", data().getItemRotation() * 45);
        tag.putFloat("ItemDropChance", data().getItemDropChance());
    }

    @Override
    protected Set<String> customKeys() {
        return Set.of("ItemRotation", "ItemDropChance");
    }

    @Override
    public void onBreak() {
        data().setItem(null);
        data().setItemRotation(0);
    }

    @Override
    public Direction getFacing() {
        return this.getLevel().getBlockState(this.getPosition()).ensureTrait(BlockTraits.FACING_DIRECTION);
    }

    @Override
    public int getItemRotation() {
        return data().getItemRotation();
    }

    @Override
    public void setItemRotation(int itemRotation) {
        if (itemRotation < 0 || itemRotation > 7) {
            throw new IllegalArgumentException("Frame rotation must be between zero and seven");
        }

        if (data().getItemRotation() != itemRotation) {
            data().setItemRotation(itemRotation);
            this.setDirty();
            this.getLevel().updateComparatorOutputLevel(this.getPosition());
        }
    }

    @Override
    public ItemStack getItem() {
        ItemStack item = data().getItem();
        return item != null ? item : ItemStack.EMPTY;
    }

    @Override
    public void setItem(ItemStack item) {
        Objects.requireNonNull(item, "item");
        ItemStack displayed = item.isEmpty() ? null : item.withCount(1);

        if (!Objects.equals(data().getItem(), displayed)) {
            data().setItem(displayed);
            if (displayed == null) {
                data().setItemRotation(0);
            }

            this.updateMapState();
            this.setDirty();
            this.getLevel().updateComparatorOutputLevel(this.getPosition());
        }
    }

    @Override
    public float getItemDropChance() {
        return data().getItemDropChance();
    }

    @Override
    public void setItemDropChance(float itemDropChance) {
        if (!Float.isFinite(itemDropChance) || itemDropChance < 0 || itemDropChance > 1) {
            throw new IllegalArgumentException("Item drop chance must be between zero and one");
        }

        if (data().getItemDropChance() != itemDropChance) {
            data().setItemDropChance(itemDropChance);
            this.setDirty();
        }
    }

    @Override
    public boolean isFixed() {
        return data().isFixed();
    }

    @Override
    public void setFixed(boolean fixed) {
        if (data().isFixed() != fixed) {
            data().setFixed(fixed);
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
                    getItem().getType() == ItemTypes.FILLED_MAP), false, true);
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
