package org.cloudburstmc.server.blockentity;

import com.nukkitx.math.vector.Vector3i;
import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtMapBuilder;
import com.nukkitx.nbt.NbtType;
import org.cloudburstmc.api.block.BlockTraits;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.blockentity.BlockEntity;
import org.cloudburstmc.api.blockentity.BlockEntityType;
import org.cloudburstmc.api.blockentity.ContainerBlockEntity;
import org.cloudburstmc.api.blockentity.Hopper;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.misc.DroppedItem;
import org.cloudburstmc.api.event.inventory.InventoryMoveItemEvent;
import org.cloudburstmc.api.inventory.Inventory;
import org.cloudburstmc.api.inventory.InventoryHolder;
import org.cloudburstmc.api.item.ItemBehaviors;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.level.chunk.Chunk;
import org.cloudburstmc.api.util.AxisAlignedBB;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.api.util.SimpleAxisAlignedBB;
import org.cloudburstmc.server.inventory.CloudHopperInventory;
import org.cloudburstmc.server.item.ItemUtils;
import org.cloudburstmc.server.player.CloudPlayer;
import org.cloudburstmc.server.registry.CloudItemRegistry;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static com.nukkitx.math.vector.Vector3i.UP;

/**
 * Created by CreeperFace on 8.5.2017.
 */
public class HopperBlockEntity extends BaseBlockEntity implements Hopper {

    protected final CloudHopperInventory inventory = new CloudHopperInventory(this);

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
    public CloudHopperInventory getInventory() {
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

                if (item == ItemStack.AIR) {
                    continue;
                }

                item = item.withCount(1);

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

            if (item == ItemStack.AIR) {
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
                itemEntity.setItem(item.withCount(items[0].getCount()));
            }
        }

        //TODO: check for minecart
        return pickedUpItem;
    }

    @Override
    public void close() {
        if (!closed) {
            for (CloudPlayer player : new HashSet<>(this.getInventory().getViewers())) {
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

                if (item == ItemStack.AIR) {
                    continue;
                }

                int[] slots = ((ContainerBlockEntity) be).getHopperPushSlots(direction, item);

                if (slots == null || slots.length == 0) {
                    continue;
                }

                for (int slot : slots) {
                    ItemStack target = inv.getItem(slot);


                    if (target != ItemStack.AIR && (!target.equals(item) ||
                            target.getCount() >= CloudItemRegistry.get().getBehavior(item.getType(), ItemBehaviors.GET_MAX_STACK_SIZE).execute())) {
                        continue;
                    }

                    item = item.withCount(1);

                    InventoryMoveItemEvent event = new InventoryMoveItemEvent(this.inventory, inv, this, item, InventoryMoveItemEvent.Action.SLOT_CHANGE);
                    this.server.getEventManager().fire(event);

                    if (event.isCancelled()) {
                        return false;
                    }

                    if (target == ItemStack.AIR) {
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
