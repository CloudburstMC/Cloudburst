package org.cloudburstmc.server.blockentity;

import org.cloudburstmc.api.block.BlockTraits;
import org.cloudburstmc.api.blockentity.BlockEntity;
import org.cloudburstmc.api.blockentity.BlockEntityType;
import org.cloudburstmc.api.blockentity.Hopper;
import org.cloudburstmc.api.blockentity.data.BlockEntityData;
import org.cloudburstmc.api.blockentity.data.HopperData;
import org.cloudburstmc.api.inventory.ItemContainer;
import org.cloudburstmc.server.container.Container;
import org.cloudburstmc.server.container.ContainerListener;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.misc.DroppedItem;
import org.cloudburstmc.api.event.inventory.InventoryMoveItemEvent;
import org.cloudburstmc.api.inventory.ItemContainer;
import org.cloudburstmc.api.inventory.view.BlockHopperView;
import org.cloudburstmc.api.inventory.view.SlotGroup;
import org.cloudburstmc.api.inventory.view.SlotGroupType;
import org.cloudburstmc.api.inventory.view.SlotGroupTypes;
import org.cloudburstmc.api.item.ItemBehaviors;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.level.chunk.Chunk;
import org.cloudburstmc.api.util.BoundingBox;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.player.CloudPlayer;
import org.cloudburstmc.server.registry.CloudItemRegistry;

import java.util.HashSet;

import static org.cloudburstmc.math.vector.Vector3i.UP;

/**
 * Block entity implementation for a hopper: a 5-slot container that automatically pulls items from
 * containers or dropped-item entities above and pushes items into containers below on each tick.
 */
public class HopperBlockEntity extends ContainerBlockEntity implements Hopper, BlockHopperView {

    private final BoundingBox pickupArea = new BoundingBox(this.getPosition(), this.getPosition().add(1, 2, 1));

    public HopperBlockEntity(BlockEntityType<?> type, Chunk chunk, Vector3i position) {
        super(type, chunk, position);
    }

    @Override
    public SlotGroupType<? extends SlotGroup> getSlotGroupType() {
        return SlotGroupTypes.HOPPER;
    }

    @Override
    public Hopper getBlockEntity() {
        return this;
    }

    @Override
    protected Class<? extends BlockEntityData> minimumDataClass() {
        return HopperData.class;
    }

    @Override
    protected HopperData data() {
        return (HopperData) super.data();
    }

    @Override
    protected ItemContainer items() {
        return data().getItems();
    }

    public String getLootTable() {
        return data().getLootTable();
    }

    public void setLootTable(String lootTable) {
        data().setLootTable(lootTable);
    }

    public Integer getLootTableSeed() {
        return data().getLootTableSeed();
    }

    public void setLootTableSeed(Integer lootTableSeed) {
        data().setLootTableSeed(lootTableSeed);
    }

    public boolean isOnTransferCooldown() {
        return data().getTransferCooldown() > 0;
    }

    public int getTransferCooldown() {
        return data().getTransferCooldown();
    }

    public void setTransferCooldown(int transferCooldown) {
        data().setTransferCooldown(transferCooldown);
    }

    @Override
    public boolean onUpdate() {
        if (this.closed) {
            return false;
        }

        if (data().getTransferCooldown() > 0) {
            data().setTransferCooldown(data().getTransferCooldown() - 1);
        }

        if (!this.isOnTransferCooldown()) {
            BlockEntity blockEntity = this.getLevel().getBlockEntity(this.getPosition().add(UP));

            boolean changed = pushItems();

            if (!changed) {
                if (blockEntity == null) {
                    changed = pickupItems();
                } else {
                    changed = pullItems();
                }
            }

            if (changed) {
                this.setTransferCooldown(8);
                setDirty();
            }
        }


        return true;
    }

    public boolean pullItems() {
        if (this.getContainer().isFull()) {
            return false;
        }

        BlockEntity blockEntity = this.getLevel().getBlockEntity(this.getPosition().add(UP));

        if (blockEntity instanceof ContainerBlockEntity containerView) {
            Container sourceContainer = containerView.getContainer();
            int[] slots = new int[sourceContainer.size()];
            for (int i = 0; i < slots.length; i++) {
                slots[i] = i;
            }

            for (int slot : slots) {
                ItemStack item = containerView.getItem(slot);

                if (item.isEmpty()) {
                    continue;
                }

                item = item.withCount(1);

                if (!this.getContainer().canAddItem(item)) {
                    continue;
                }

                InventoryMoveItemEvent ev = InventoryMoveItemEvent.transfer((SlotGroup) containerView, this, this, item);
                this.server.getEventManager().fire(ev);

                if (ev.isCancelled()) {
                    return false;
                }

                ItemStack[] items = this.getContainer().addItem(item);

                if (items.length <= 0) {
                    containerView.getContainer().decrementCount(slot);
                    return true;
                }
            }
        }
        return false;
    }

    public boolean pickupItems() {
        if (this.getContainer().isFull()) {
            return false;
        }

        boolean pickedUpItem = false;

        for (Entity entity : this.getLevel().getCollidingEntities(this.pickupArea)) {
            if (entity.isClosed() || !(entity instanceof DroppedItem itemEntity)) {
                continue;
            }

            ItemStack item = itemEntity.getItem();

            if (item.isEmpty()) {
                continue;
            }

            int originalCount = item.getCount();

            if (!this.getContainer().canAddItem(item)) {
                continue;
            }

            InventoryMoveItemEvent ev = InventoryMoveItemEvent.transferWithWorld(null, this, this, item);
            this.server.getEventManager().fire(ev);

            if (ev.isCancelled()) {
                continue;
            }

            ItemStack[] items = this.getContainer().addItem(item);

            if (items.length == 0) {
                entity.close();
                pickedUpItem = true;
                continue;
            }

            if (items[0].getCount() != originalCount) {
                pickedUpItem = true;
                itemEntity.setItem(item.withCount(items[0].getCount()));
            }
        }

        //TODO: check for minecart
        return pickedUpItem;
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
    public void onBreak() {
        for (ItemStack content : getContainer().getContents()) {
            this.dropItemOnBreak(content);
        }
    }

    public boolean pushItems() {
        if (this.getContainer().isEmpty()) {
            return false;
        }

        Direction direction = getBlockState().ensureTrait(BlockTraits.FACING_DIRECTION);
        BlockEntity be = this.getLevel().getBlockEntity(direction.relative(this.getPosition()));

        if (be instanceof Hopper && direction == Direction.DOWN || !(be instanceof ContainerBlockEntity))
            return false;

        if (be instanceof ContainerBlockEntity containerView) {
            Container container = containerView.getContainer();

            for (int i = 0; i < this.getContainer().size(); i++) {
                ItemStack item = this.getContainer().getItem(i);

                if (item.isEmpty()) {
                    continue;
                }

                int maxStackSize = CloudItemRegistry.get().requireComponent(item.getType(), ItemBehaviors.GET_MAX_STACK_SIZE).execute(item);
                int firstEmpty = -1;
                int firstPartial = -1;
                for (int s = 0; s < container.size(); s++) {
                    ItemStack target = container.getItem(s);
                    if (target.isEmpty()) {
                        if (firstEmpty == -1) firstEmpty = s;
                    } else if (target.equals(item) && target.getCount() < maxStackSize) {
                        if (firstPartial == -1) firstPartial = s;
                    }
                }
                int slot = firstPartial != -1 ? firstPartial : firstEmpty;

                if (slot == -1) {
                    continue;
                }

                ItemStack target = container.getItem(slot);
                item = item.withCount(1);

                InventoryMoveItemEvent event = InventoryMoveItemEvent.transfer(this, (SlotGroup) containerView, this, item);
                this.server.getEventManager().fire(event);

                if (event.isCancelled()) {
                    return false;
                }

                if (target.isEmpty()) {
                    container.setItem(slot, item);
                } else {
                    container.incrementCount(slot);
                }

                this.getContainer().decrementCount(i);
                return true;
            }
        }

        //TODO: check for minecart
        return false;
    }

    @Override
    public boolean isSpawnable() {
        return true;
    }
}
