package org.cloudburstmc.server.blockentity;

import org.cloudburstmc.api.block.BlockTraits;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.blockentity.BlockEntity;
import org.cloudburstmc.api.blockentity.BlockEntityType;
import org.cloudburstmc.api.blockentity.Hopper;
import org.cloudburstmc.api.container.Container;
import org.cloudburstmc.api.container.ContainerListener;
import org.cloudburstmc.api.container.ContainerViewTypes;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.misc.DroppedItem;
import org.cloudburstmc.api.event.inventory.InventoryMoveItemEvent;
import org.cloudburstmc.api.item.ItemBehaviors;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.level.chunk.Chunk;
import org.cloudburstmc.api.util.AxisAlignedBB;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.api.util.SimpleAxisAlignedBB;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.server.container.CloudContainer;
import org.cloudburstmc.server.item.ItemUtils;
import org.cloudburstmc.server.player.CloudPlayer;
import org.cloudburstmc.server.registry.CloudItemRegistry;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static org.cloudburstmc.math.vector.Vector3i.UP;

/**
 * Created by CreeperFace on 8.5.2017.
 */
public class HopperBlockEntity extends ContainerBlockEntity implements Hopper {

    private final AxisAlignedBB pickupArea = new SimpleAxisAlignedBB(this.getPosition(), this.getPosition().add(1, 2, 1));
    private int transferCooldown = 8;

    public HopperBlockEntity(BlockEntityType<?> type, Chunk chunk, Vector3i position) {
        super(type, chunk, position, new CloudContainer(5), ContainerViewTypes.HOPPER);
    }

    @Override
    public void loadAdditionalData(NbtMap tag) {
        super.loadAdditionalData(tag);

        tag.listenForList("Items", NbtType.COMPOUND, tags -> {
            for (NbtMap itemTag : tags) {
                this.container.setItem(itemTag.getByte("Slot"), ItemUtils.deserializeItem(itemTag));
            }
        });
        tag.listenForInt("TransferCooldown", this::setTransferCooldown);

        this.scheduleUpdate();
    }

    @Override
    public void saveAdditionalData(NbtMapBuilder tag) {
        super.saveAdditionalData(tag);

        List<NbtMap> items = new ArrayList<>();
        this.container.forEachSlot((itemStack, slot) -> items.add(ItemUtils.serializeItem(itemStack, slot)));
        tag.putList("Items", NbtType.COMPOUND, items);

        tag.putInt("TransferCooldown", this.transferCooldown);
    }

    @Override
    public boolean isValid() {
        return getBlockState().getType() == BlockTypes.HOPPER;
    }

    public boolean isOnTransferCooldown() {
        return this.transferCooldown > 0;
    }

    public void setTransferCooldown(int transferCooldown) {
        this.transferCooldown = transferCooldown;
    }

    @Override
    public boolean onUpdate() {
        if (this.closed) {
            return false;
        }

        if (this.transferCooldown > 0) {
            this.transferCooldown--;
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
        if (this.container.isFull()) {
            return false;
        }

        BlockEntity blockEntity = this.getLevel().getBlockEntity(this.getPosition().add(UP));

        if (blockEntity instanceof ContainerBlockEntity containerView) {
//            int[] slots = containerView.getHopperPullSlots();
            int[] slots = new int[0];

            if (slots == null || slots.length == 0) {
                return false;
            }

            for (int slot : slots) {
                ItemStack item = containerView.getItem(slot);

                if (item == ItemStack.EMPTY) {
                    continue;
                }

                item = item.withCount(1);

                if (!this.container.canAddItem(item)) {
                    continue;
                }

                InventoryMoveItemEvent ev = new InventoryMoveItemEvent(containerView, this, item, InventoryMoveItemEvent.Action.SLOT_CHANGE);
                this.server.getEventManager().fire(ev);

                if (ev.isCancelled()) {
                    return false;
                }

                ItemStack[] items = this.container.addItem(item);

                if (items.length <= 0) {
                    containerView.getContainer().decrementCount(slot);
                    return true;
                }
            }
        }
        return false;
    }

    public boolean pickupItems() {
        if (this.container.isFull()) {
            return false;
        }

        boolean pickedUpItem = false;

        for (Entity entity : this.getLevel().getCollidingEntities(this.pickupArea)) {
            if (entity.isClosed() || !(entity instanceof DroppedItem)) {
                continue;
            }

            DroppedItem itemEntity = (DroppedItem) entity;
            ItemStack item = itemEntity.getItem();

            if (item == ItemStack.EMPTY) {
                continue;
            }

            int originalCount = item.getCount();

            if (!this.container.canAddItem(item)) {
                continue;
            }

            InventoryMoveItemEvent ev = new InventoryMoveItemEvent(null, this, item, InventoryMoveItemEvent.Action.PICKUP);
            this.server.getEventManager().fire(ev);

            if (ev.isCancelled()) {
                continue;
            }

            ItemStack[] items = this.container.addItem(item);

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
            for (ContainerListener listener : new HashSet<>(this.container.getListeners())) {
                if (listener instanceof CloudPlayer) {
                    ((CloudPlayer) listener).getInventoryManager().closeScreen();
                }
            }
            super.close();
        }
    }

    @Override
    public void onBreak() {
        for (ItemStack content : container.getContents()) {
            this.getLevel().dropItem(this.getPosition(), content);
        }
    }

    public boolean pushItems() {
        if (this.container.isEmpty()) {
            return false;
        }

        Direction direction = getBlockState().ensureTrait(BlockTraits.FACING_DIRECTION);
        BlockEntity be = this.getLevel().getBlockEntity(direction.getOffset(this.getPosition()));

        if (be instanceof Hopper && direction == Direction.DOWN || !(be instanceof ContainerBlockEntity))
            return false;

        if (be instanceof ContainerBlockEntity containerView) {
            Container container = containerView.getContainer();

            for (int i = 0; i < this.container.size(); i++) {
                ItemStack item = this.container.getItem(i);

                if (item == ItemStack.EMPTY) {
                    continue;
                }

//                int[] slots = ((ContainerBlockEntity) be).getHopperPushSlots(direction, item);
                int[] slots = new int[0];

                if (slots == null || slots.length == 0) {
                    continue;
                }

                for (int slot : slots) {
                    ItemStack target = container.getItem(slot);


                    if (target != ItemStack.EMPTY && (!target.equals(item) ||
                            target.getCount() >= CloudItemRegistry.get().getBehavior(item.getType(), ItemBehaviors.GET_MAX_STACK_SIZE).execute())) {
                        continue;
                    }

                    item = item.withCount(1);

                    InventoryMoveItemEvent event = new InventoryMoveItemEvent(this, containerView, item, InventoryMoveItemEvent.Action.SLOT_CHANGE);
                    this.server.getEventManager().fire(event);

                    if (event.isCancelled()) {
                        return false;
                    }

                    if (target == ItemStack.EMPTY) {
                        container.setItem(slot, item);
                    } else {
                        container.incrementCount(slot);
                    }

                    this.container.decrementCount(i);
                    return true;
                }
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
