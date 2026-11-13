package org.cloudburstmc.server.blockentity;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.BlockTraits;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.blockentity.BlockEntity;
import org.cloudburstmc.api.blockentity.BlockEntityType;
import org.cloudburstmc.api.blockentity.Hopper;
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
import org.cloudburstmc.server.block.component.ComposterBlockHandlers;
import org.cloudburstmc.server.block.util.RedstoneSignals;
import org.cloudburstmc.server.blockentity.data.CloudHopperData;
import org.cloudburstmc.server.container.ContainerListener;
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

    private final CloudHopperData persistenceState;

    public HopperBlockEntity(BlockEntityType<?> type, Chunk chunk, Vector3i position) {
        CloudHopperData state = new CloudHopperData();
        super(type, chunk, position, state);
        this.persistenceState = state;
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
    protected CloudHopperData data() {
        return this.persistenceState;
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
        if (this.isClosed()) {
            return false;
        }

        if (data().getTransferCooldown() > 0) {
            data().setTransferCooldown(data().getTransferCooldown() - 1);
        }

        if (!this.isOnTransferCooldown() && !RedstoneSignals.hasNeighborSignal(this.getBlock())) {
            Block above = this.getLevel().getLoadedBlock(this.getPosition().add(UP));
            BlockEntity blockEntity = this.getLevel().getBlockEntity(this.getPosition().add(UP));

            boolean changed = pushItems();

            if (!changed) {
                if (above != null && above.getState().getType() == BlockTypes.COMPOSTER) {
                    changed = ComposterBlockHandlers.extractFromBelow(above, this, this.getContainer());
                } else if (blockEntity == null) {
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

        if (blockEntity instanceof AutomatedBlockStorage containerView) {
            int[] slots = new int[containerView.insertionSlots().size()];
            for (int i = 0; i < slots.length; i++) {
                slots[i] = i;
            }

            for (int slot : slots) {
                if (containerView.blocksExtractionBelow(slot)) {
                    continue;
                }
                ItemStack sourceStack = containerView.getItem(slot);

                if (sourceStack.isEmpty()) {
                    continue;
                }

                ItemStack item = sourceStack.withCount(1);

                if (!this.getContainer().canAddItem(item)) {
                    continue;
                }

                InventoryMoveItemEvent ev = InventoryMoveItemEvent.transfer((SlotGroup) containerView, this, this, item);
                this.server.getEventManager().fire(ev);

                if (ev.isCancelled() || this.isClosed() || containerView.isClosed()
                        || containerView.blocksExtractionBelow(slot) || !sourceStack.equals(containerView.getItem(slot))) {
                    return false;
                }

                ItemStack[] items = this.getContainer().addItem(item);

                if (items.length <= 0) {
                    containerView.setItem(slot, sourceStack.decreaseCount());
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
        if (!this.isClosed()) {
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
        Vector3i targetPosition = direction.relative(this.getPosition());
        if (direction == Direction.DOWN) {
            Block target = this.getLevel().getLoadedBlock(targetPosition);
            if (target != null && target.getState().getType() == BlockTypes.COMPOSTER) {
                return ComposterBlockHandlers.insertFromAbove(target, this, this.getContainer());
            }
        }

        BlockEntity be = this.getLevel().getBlockEntity(targetPosition);

        if (be instanceof Hopper && direction == Direction.DOWN)
            return false;

        if (be instanceof AutomatedBlockStorage containerView) {
            ItemContainer container = containerView.insertionSlots();

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
                    int count = target.getCount() + 1;
                    if ((!target.isEmpty() && !target.isStackableWith(item)) || count > maxStackSize
                            || containerView.blocksInsertionFrom(s, direction.getOpposite(), item.withCount(count))) {
                        continue;
                    }
                    if (target.isEmpty()) {
                        if (firstEmpty == -1) firstEmpty = s;
                    } else if (target.isStackableWith(item) && target.getCount() < maxStackSize) {
                        if (firstPartial == -1) firstPartial = s;
                    }
                }
                int slot = firstPartial != -1 ? firstPartial : firstEmpty;

                if (slot == -1) {
                    continue;
                }

                ItemStack target = container.getItem(slot);
                ItemStack sourceStack = item;
                item = item.withCount(1);

                InventoryMoveItemEvent event = InventoryMoveItemEvent.transfer(this, (SlotGroup) containerView, this, item);
                this.server.getEventManager().fire(event);

                ItemStack updated = target.isEmpty() ? item : target.withCount(target.getCount() + 1);
                if (event.isCancelled() || this.isClosed() || containerView.isClosed()
                        || !sourceStack.equals(this.getContainer().getItem(i)) || !target.equals(container.getItem(slot))
                        || containerView.blocksInsertionFrom(slot, direction.getOpposite(), updated)) {
                    return false;
                }

                container.setItem(slot, updated);

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
