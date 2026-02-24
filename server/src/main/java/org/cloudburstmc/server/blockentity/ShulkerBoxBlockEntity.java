package org.cloudburstmc.server.blockentity;

import org.cloudburstmc.api.block.BlockType;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.blockentity.BlockEntityType;
import org.cloudburstmc.api.blockentity.ShulkerBox;
import org.cloudburstmc.server.container.ContainerListener;
import org.cloudburstmc.api.inventory.view.BlockStorageView;
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
import org.cloudburstmc.server.player.CloudPlayer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Block entity implementation for a shulker box: a 27-slot storage container with an associated
 * facing direction that only accepts valid shulker box block types.
 */
public class ShulkerBoxBlockEntity extends ContainerBlockEntity implements ShulkerBox, BlockStorageView {
    private byte facing;

    public ShulkerBoxBlockEntity(BlockEntityType<?> type, Chunk chunk, Vector3i position) {
        super(type, chunk, position, new CloudContainer(27));
    }

    @Override
    public SlotGroupType<? extends SlotGroup> getSlotGroupType() {
        return SlotGroupTypes.SHULKER_BOX;
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
        this.facing = tag.getByte("facing");
    }

    @Override
    public void saveAdditionalData(NbtMapBuilder tag) {
        super.saveAdditionalData(tag);

        List<NbtMap> items = new ArrayList<>();
        this.container.forEachSlot((itemStack, slot) -> items.add(ItemUtils.serializeItem(itemStack, slot)));
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
            for (ContainerListener listener : new HashSet<>(this.container.getListeners())) {
                if (listener instanceof CloudPlayer) {
                    ((CloudPlayer) listener).closeInventory();
                }
            }
            super.close();
        }
    }

    @Override
    public boolean isValid() {
        BlockType type = this.getBlockState().getType();
        return type == BlockTypes.UNDYED_SHULKER_BOX ||
                type == BlockTypes.WHITE_SHULKER_BOX ||
                type == BlockTypes.ORANGE_SHULKER_BOX ||
                type == BlockTypes.MAGENTA_SHULKER_BOX ||
                type == BlockTypes.LIGHT_BLUE_SHULKER_BOX ||
                type == BlockTypes.YELLOW_SHULKER_BOX ||
                type == BlockTypes.LIME_SHULKER_BOX ||
                type == BlockTypes.PINK_SHULKER_BOX ||
                type == BlockTypes.GRAY_SHULKER_BOX ||
                type == BlockTypes.LIGHT_GRAY_SHULKER_BOX ||
                type == BlockTypes.CYAN_SHULKER_BOX ||
                type == BlockTypes.PURPLE_SHULKER_BOX ||
                type == BlockTypes.BLUE_SHULKER_BOX ||
                type == BlockTypes.BROWN_SHULKER_BOX ||
                type == BlockTypes.GREEN_SHULKER_BOX ||
                type == BlockTypes.RED_SHULKER_BOX ||
                type == BlockTypes.BLACK_SHULKER_BOX;
    }

    @Override
    public boolean isSpawnable() {
        return true;
    }
}
