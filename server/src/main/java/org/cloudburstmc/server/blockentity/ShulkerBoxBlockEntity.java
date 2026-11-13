package org.cloudburstmc.server.blockentity;

import org.cloudburstmc.api.blockentity.data.ShulkerBoxData;
import org.cloudburstmc.api.blockentity.data.BlockEntityData;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.cloudburstmc.api.blockentity.BlockEntityType;
import org.cloudburstmc.api.blockentity.ShulkerBox;
import org.cloudburstmc.api.blockentity.ShulkerBoxAnimationState;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.PushReaction;
import org.cloudburstmc.api.inventory.view.BlockStorageView;
import org.cloudburstmc.api.inventory.view.SlotGroup;
import org.cloudburstmc.api.inventory.view.SlotGroupType;
import org.cloudburstmc.api.inventory.view.SlotGroupTypes;
import org.cloudburstmc.api.item.ItemDataComponents;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemStackBuilder;
import org.cloudburstmc.api.item.data.ShulkerBoxContents;
import org.cloudburstmc.api.level.chunk.Chunk;
import org.cloudburstmc.api.level.sound.SoundTypes;
import org.cloudburstmc.api.util.BoundingBox;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.api.util.MovementType;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.protocol.bedrock.packet.BlockEventPacket;
import org.cloudburstmc.server.block.util.ShulkerBoxGeometry;
import org.cloudburstmc.server.container.CloudContainer;
import org.cloudburstmc.server.container.ContainerListener;
import org.cloudburstmc.server.entity.CloudEntity;
import org.cloudburstmc.server.item.ShulkerBoxItems;
import org.cloudburstmc.server.level.chunk.CloudChunk;
import org.cloudburstmc.server.player.CloudPlayer;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Stores the inventory and lid state of a placed shulker box.
 */
public class ShulkerBoxBlockEntity extends ContainerBlockEntity implements ShulkerBox, BlockStorageView {
    private static final int LID_TRAVEL_TICKS = 20;
    private static final float COLLISION_EPSILON = 1.0E-6f;

    private ShulkerBoxAnimationState animationState = ShulkerBoxAnimationState.CLOSED;
    private int lidTick;
    private float openProgress;
    private float previousOpenProgress;
    private int viewerCount;

    public ShulkerBoxBlockEntity(BlockEntityType<?> type, Chunk chunk, Vector3i position) {
        super(type, chunk, position);
        this.data().setItems(new BoxStorage((CloudChunk) chunk));
    }

    @Override
    protected Class<? extends BlockEntityData> minimumDataClass() {
        return ShulkerBoxData.class;
    }

    @Override
    protected ShulkerBoxData data() {
        return (ShulkerBoxData) super.data();
    }

    @Override
    protected org.cloudburstmc.api.inventory.ItemContainer items() {
        return this.data().getItems();
    }

    @Override
    public SlotGroupType<? extends SlotGroup> getSlotGroupType() {
        return SlotGroupTypes.SHULKER_BOX;
    }

    @Override
    protected void loadCustom(NbtMap tag) {
        if (!tag.containsKey("facing")) {
            this.data().setFacing(Direction.DOWN);
        }
    }

    @Override
    protected void saveCustomClient(NbtMapBuilder tag) {
        super.saveCustomClient(tag);
        this.saveFacing(tag);
    }

    private void saveFacing(NbtMapBuilder tag) {
        tag.putByte("facing", (byte) this.data().getFacing().getIndex());
    }

    @Override
    public void applyPlacementItem(ItemStack item) {
        super.applyPlacementItem(item);
        this.setCustomName(item.get(ItemDataComponents.CUSTOM_NAME));
        this.restoreContents(item.getOrDefault(ItemDataComponents.SHULKER_BOX_CONTENTS, new ShulkerBoxContents(Map.of())));
    }

    public ItemStack asItem() {
        ItemStackBuilder item = ItemStack.from(this.getBlock().getState().getType().getDefaultState()).toBuilder();
        ShulkerBoxContents contents = this.snapshotContents();
        if (!contents.items().isEmpty()) {
            item.setData(ItemDataComponents.SHULKER_BOX_CONTENTS, contents);
        }

        if (this.hasCustomName()) {
            item.setData(ItemDataComponents.CUSTOM_NAME, this.getCustomName());
        }

        return item.build();
    }

    private ShulkerBoxContents snapshotContents() {
        Map<Integer, ItemStack> items = new LinkedHashMap<>();
        this.getContainer().forEachSlot((item, slot) -> {
            if (!item.isEmpty()) {
                items.put(slot, item);
            }
        });
        return new ShulkerBoxContents(items);
    }

    private void restoreContents(ShulkerBoxContents contents) {
        for (int slot = 0; slot < this.getContainer().size(); slot++) {
            ItemStack item = contents.items().getOrDefault(slot, ItemStack.EMPTY);
            this.getContainer().setItem(slot, ShulkerBoxItems.accepts(item) ? item : ItemStack.EMPTY);
        }
    }

    @Override
    public boolean canInsertFrom(int slot, Direction side, ItemStack item) {
        return ShulkerBoxItems.accepts(item);
    }

    @Override
    public Direction getFacing() {
        return this.data().getFacing();
    }

    public void setFacing(Direction facing) {
        this.data().setFacing(Objects.requireNonNull(facing, "facing"));
        this.setDirty();
    }

    @Override
    public ShulkerBoxAnimationState getAnimationState() {
        return this.animationState;
    }

    @Override
    public float getOpenProgress() {
        return this.openProgress;
    }

    @Override
    public float getPreviousOpenProgress() {
        return this.previousOpenProgress;
    }

    @Override
    public int getViewerCount() {
        return this.viewerCount;
    }

    public void startOpen() {
        this.viewerCount++;
        if (this.viewerCount == 1) {
            this.sendOpenState();
            this.animationState = ShulkerBoxAnimationState.OPENING;
            this.getLevel().playSound(this.getPosition(), SoundTypes.RANDOM_SHULKERBOXOPEN, 0.5f, randomPitch());
            this.scheduleUpdate();
        }
    }

    public void stopOpen() {
        if (this.viewerCount == 0) {
            return;
        }

        this.viewerCount--;
        if (this.viewerCount == 0) {
            this.sendOpenState();
            this.animationState = ShulkerBoxAnimationState.CLOSING;
            this.getLevel().playSound(this.getPosition(), SoundTypes.RANDOM_SHULKERBOXCLOSED, 0.5f, randomPitch());
            this.scheduleUpdate();
        }
    }

    public boolean isOpeningBlocked() {
        if (this.animationState != ShulkerBoxAnimationState.CLOSED) {
            return false;
        }

        BoundingBox openingSweep = ShulkerBoxGeometry.sweptLid(this.getPosition(), this.data().getFacing(), 0, 0.5f)
                .deflate(COLLISION_EPSILON, COLLISION_EPSILON, COLLISION_EPSILON);
        return this.getLevel().hasCollision(null, openingSweep);
    }

    @Override
    public boolean onUpdate() {
        super.onUpdate();
        this.previousOpenProgress = this.openProgress;

        switch (this.animationState) {
            case OPENING -> {
                if (this.lidTick == 0) {
                    this.getLevel().updateAround(this.getPosition());
                }

                this.advanceLid(1);
                if (this.openProgress > this.previousOpenProgress) {
                    this.pushEntities();
                }

                if (this.lidTick == LID_TRAVEL_TICKS) {
                    this.animationState = ShulkerBoxAnimationState.OPEN;
                    this.getLevel().updateAround(this.getPosition());
                }
            }
            case CLOSING -> {
                if (this.lidTick == LID_TRAVEL_TICKS) {
                    this.getLevel().updateAround(this.getPosition());
                }

                this.advanceLid(-1);
                if (this.lidTick == 0) {
                    this.animationState = ShulkerBoxAnimationState.CLOSED;
                    this.getLevel().updateAround(this.getPosition());
                }
            }
            case CLOSED, OPEN -> {
            }
        }

        return this.animationState != ShulkerBoxAnimationState.CLOSED;
    }

    private void advanceLid(int step) {
        this.lidTick = Math.clamp(this.lidTick + step, 0, LID_TRAVEL_TICKS);
        float remaining = (LID_TRAVEL_TICKS - this.lidTick) / (float) LID_TRAVEL_TICKS;
        this.openProgress = 1 - remaining * remaining * remaining;
    }

    private void pushEntities() {
        float previousExtension = this.previousOpenProgress * 0.5f;
        float extension = this.openProgress * 0.5f;
        BoundingBox box = ShulkerBoxGeometry.sweptLid(this.getPosition(), this.data().getFacing(), previousExtension, extension);
        float distance = extension - previousExtension + 0.01f;
        for (Entity entity : this.getLevel().getNearbyEntities(box)) {
            if (entity.getPushReaction() != PushReaction.IGNORE && entity instanceof CloudEntity cloudEntity) {
                cloudEntity.move(MovementType.SHULKER_BOX,
                        distance * this.data().getFacing().getStepX(),
                        distance * this.data().getFacing().getStepY(),
                        distance * this.data().getFacing().getStepZ()
                );
            }
        }
    }

    private void sendOpenState() {
        BlockEventPacket packet = new BlockEventPacket();
        packet.setBlockPosition(this.getPosition());
        packet.setEventType(1);
        packet.setEventData(this.viewerCount > 0 ? 1 : 0);
        this.getLevel().addChunkPacket(this.getPosition(), packet);
    }

    private static float randomPitch() {
        return ThreadLocalRandom.current().nextFloat(0.9f, 1.0f);
    }

    @Override
    public void close() {
        if (!closed) {
            for (ContainerListener listener : new HashSet<>(this.getContainer().getListeners())) {
                if (listener instanceof CloudPlayer) {
                    ((CloudPlayer) listener).closeInventory();
                }
            }

            super.close();
        }
    }

    @Override
    public boolean isSpawnable() {
        return true;
    }

    private static class BoxStorage extends CloudContainer {
        private final CloudChunk chunk;

        private BoxStorage(CloudChunk chunk) {
            super(ShulkerBoxContents.SLOT_COUNT);
            this.chunk = chunk;
        }

        @Override
        public void setItem(int slot, @NonNull ItemStack item) {
            if (!ShulkerBoxItems.accepts(item)) {
                throw new IllegalArgumentException("Shulker boxes cannot be stored in shulker boxes");
            }
            super.setItem(slot, item);
        }

        @Override
        protected void onSlotChange(int slot, ItemStack previous) {
            this.chunk.markDirty();
            super.onSlotChange(slot, previous);
        }
    }
}
