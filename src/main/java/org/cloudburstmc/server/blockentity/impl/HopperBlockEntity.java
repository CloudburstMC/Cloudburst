package org.cloudburstmc.server.blockentity.impl;

import com.nukkitx.math.vector.Vector3i;
import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtMapBuilder;
import com.nukkitx.nbt.NbtType;
import org.cloudburstmc.server.block.BlockTraits;
import org.cloudburstmc.server.block.BlockTypes;
import org.cloudburstmc.server.blockentity.BlockEntity;
import org.cloudburstmc.server.blockentity.BlockEntityType;
import org.cloudburstmc.server.blockentity.ContainerBlockEntity;
import org.cloudburstmc.server.blockentity.Hopper;
import org.cloudburstmc.server.entity.Entity;
import org.cloudburstmc.server.entity.misc.DroppedItem;
import org.cloudburstmc.server.event.inventory.InventoryMoveItemEvent;
import org.cloudburstmc.server.inventory.HopperInventory;
import org.cloudburstmc.server.inventory.Inventory;
import org.cloudburstmc.server.inventory.InventoryHolder;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.item.ItemUtils;
import org.cloudburstmc.server.level.chunk.Chunk;
import org.cloudburstmc.server.math.AxisAlignedBB;
import org.cloudburstmc.server.math.Direction;
import org.cloudburstmc.server.math.SimpleAxisAlignedBB;
import org.cloudburstmc.server.player.Player;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static com.nukkitx.math.vector.Vector3i.UP;

/**
 * Created by CreeperFace on 8.5.2017.
 */
public class HopperBlockEntity extends BaseBlockEntity implements Hopper {

    protected final HopperInventory inventory = new HopperInventory(this);

    private final AxisAlignedBB pickupArea = new SimpleAxisAlignedBB(this.getPosition(), this.getPosition().add(1, 2, 1));
    private int transferCooldown = 8;

    public HopperBlockEntity(BlockEntityType<?> type, Chunk chunk, Vector3i position) {
        super(type, chunk, position);
    }

    @Override
    public void loadAdditionalData(NbtMap tag) {
        super.loadAdditionalData(tag);

        tag.listenForList("Items", NbtType.COMPOUND, tags -> {
            for (NbtMap itemTag : tags) {
                this.inventory.setItem(itemTag.getByte("Slot"), ItemUtils.deserializeItem(itemTag));
            }
        });
        tag.listenForInt("TransferCooldown", this::setTransferCooldown);

        this.scheduleUpdate();
    }

    @Override
    public void saveAdditionalData(NbtMapBuilder tag) {
        super.saveAdditionalData(tag);

        List<NbtMap> items = new ArrayList<>();
        for (Map.Entry<Integer, ItemStack> entry : this.inventory.getContents().entrySet()) {
            items.add(ItemUtils.serializeItem(entry.getValue(), entry.getKey()));
        }
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
    public HopperInventory getInventory() {
        return inventory;
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
        if (this.inventory.isFull()) {
            return false;
        }

        BlockEntity blockEntity = this.getLevel().getBlockEntity(this.getPosition().add(UP));

        if (blockEntity instanceof ContainerBlockEntity) {
            Inventory inv = ((ContainerBlockEntity) blockEntity).getInventory();
            int[] slots = ((ContainerBlockEntity) blockEntity).getHopperPullSlots();

            if (slots == null || slots.length == 0) {
                return false;
            }

            for (int slot : slots) {
                ItemStack item = inv.getItem(slot);

                if (item.isNull()) {
                    continue;
                }

                item = item.withAmount(1);

                if (!this.inventory.canAddItem(item)) {
                    continue;
                }

                InventoryMoveItemEvent ev = new InventoryMoveItemEvent(inv, this.inventory, this, item, InventoryMoveItemEvent.Action.SLOT_CHANGE);
                this.server.getEventManager().fire(ev);

                if (ev.isCancelled()) {
                    return false;
                }

                ItemStack[] items = this.inventory.addItem(item);

                if (items.length <= 0) {
                    inv.decrementCount(slot);
                    return true;
                }
            }
        }
        return false;
    }

    public boolean pickupItems() {
        if (this.inventory.isFull()) {
            return false;
        }

        boolean pickedUpItem = false;

        for (Entity entity : this.getLevel().getCollidingEntities(this.pickupArea)) {
            if (entity.isClosed() || !(entity instanceof DroppedItem)) {
                continue;
            }

            DroppedItem itemEntity = (DroppedItem) entity;
            ItemStack item = itemEntity.getItem();

            if (item.isNull()) {
                continue;
            }

            int originalCount = item.getCount();

            if (!this.inventory.canAddItem(item)) {
                continue;
            }

            InventoryMoveItemEvent ev = new InventoryMoveItemEvent(null, this.inventory, this, item, InventoryMoveItemEvent.Action.PICKUP);
            this.server.getEventManager().fire(ev);

            if (ev.isCancelled()) {
                continue;
            }

            ItemStack[] items = this.inventory.addItem(item);

            if (items.length == 0) {
                entity.close();
                pickedUpItem = true;
                continue;
            }

            if (items[0].getCount() != originalCount) {
                pickedUpItem = true;
                itemEntity.setItem(item.withAmount(items[0].getAmount()));
            }
        }

        //TODO: check for minecart
        return pickedUpItem;
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
    public void onBreak() {

        for (ItemStack content : inventory.getContents().values()) {
            this.getLevel().dropItem(this.getPosition(), content);
        }
    }

    public boolean pushItems() {
        if (this.inventory.isEmpty()) {
            return false;
        }

        Direction direction = getBlockState().ensureTrait(BlockTraits.FACING_DIRECTION);
        BlockEntity be = this.getLevel().getBlockEntity(direction.getOffset(this.getPosition()));

        if (be instanceof Hopper && direction == Direction.DOWN || !(be instanceof InventoryHolder))
            return false;

        if (be instanceof ContainerBlockEntity) {
            Inventory inv = ((ContainerBlockEntity) be).getInventory();

            for (int i = 0; i < this.inventory.getSize(); i++) {
                ItemStack item = this.inventory.getItem(i);

                if (item.isNull()) {
                    continue;
                }

                int[] slots = ((ContainerBlockEntity) be).getHopperPushSlots(direction, item);

                if (slots == null || slots.length == 0) {
                    continue;
                }

                for (int slot : slots) {
                    ItemStack target = inv.getItem(slot);

                    if (!target.isNull() && (!target.equals(item) || target.getCount() >= item.getBehavior().getMaxStackSize(item))) {
                        continue;
                    }

                    item = item.withAmount(1);

                    InventoryMoveItemEvent event = new InventoryMoveItemEvent(this.inventory, inv, this, item, InventoryMoveItemEvent.Action.SLOT_CHANGE);
                    this.server.getEventManager().fire(event);

                    if (event.isCancelled()) {
                        return false;
                    }

                    if (target.isNull()) {
                        inv.setItem(slot, item);
                    } else {
                        inv.incrementCount(slot);
                    }

                    inventory.decrementCount(i);
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
