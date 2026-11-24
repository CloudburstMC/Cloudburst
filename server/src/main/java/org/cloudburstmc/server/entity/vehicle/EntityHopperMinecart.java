package org.cloudburstmc.server.entity.vehicle;

import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.misc.DroppedItem;
import org.cloudburstmc.api.entity.vehicle.ChestBoat;
import org.cloudburstmc.api.entity.vehicle.ContainerMinecart;
import org.cloudburstmc.api.entity.vehicle.HopperMinecart;
import org.cloudburstmc.api.event.EventManager;
import org.cloudburstmc.api.event.entity.EntityRemoveCause;
import org.cloudburstmc.api.event.inventory.InventoryMoveItemEvent;
import org.cloudburstmc.api.inventory.view.SlotGroup;
import org.cloudburstmc.api.inventory.view.StorageView;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemType;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.util.BoundingBox;
import org.cloudburstmc.api.util.data.MinecartType;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerType;
import org.cloudburstmc.server.blockentity.AutomatedBlockStorage;
import org.cloudburstmc.server.entity.CloudEntity;
import org.cloudburstmc.server.entity.data.CloudHopperMinecartData;
import org.cloudburstmc.server.level.CloudLevel;

import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.IntPredicate;

public class EntityHopperMinecart extends EntityContainerMinecart implements HopperMinecart {

    public EntityHopperMinecart(EntityType<HopperMinecart> type, Location location) {
        super(type, location, 5, ContainerType.MINECART_HOPPER, ContainerType.HOPPER, new CloudHopperMinecartData());
    }

    @Override
    protected CloudHopperMinecartData data() {
        return (CloudHopperMinecartData) super.data();
    }

    @Override
    protected void loadCustom(NbtMap tag) {
        super.loadCustom(tag);
        if (!tag.containsKey("Enabled")) {
            this.data().setEnabled(null);
        }
    }

    @Override
    protected void initEntity() {
        super.initEntity();
        this.setDisplayBlock(BlockTypes.HOPPER.getDefaultState());
        this.setDisplay(true);
    }

    @Override
    public MinecartType getMinecartType() {
        return MinecartType.MINECART_HOPPER;
    }

    @Override
    protected ItemType getDropItemType() {
        return ItemTypes.HOPPER_MINECART;
    }

    @Override
    public boolean isEnabled() {
        return !Boolean.FALSE.equals(this.data().getEnabled());
    }

    @Override
    public void setEnabled(boolean enabled) {
        if (this.isEnabled() != enabled) {
            this.data().setEnabled(enabled ? null : Boolean.FALSE);
            this.markChunkDirty();
        }
    }

    @Override
    protected void activate(int x, int y, int z, boolean powered) {
        this.setEnabled(!powered);
    }

    @Override
    protected boolean tickEntity(int currentTick, int elapsedTicks) {
        boolean active = super.tickEntity(currentTick, elapsedTicks);
        if (active && this.canCollect() && !this.inventoryContainer().isFull()) {
            this.suckInItems(this.getServer().getEventManager());
        }
        return active;
    }

    private boolean canCollect() {
        return this.isEnabled() && !this.isClosed() && this.isAlive();
    }

    private void suckInItems(EventManager events) {
        CloudLevel level = this.getLevel();
        Vector3f position = this.getPosition();
        Vector3i above = Vector3i.from(position.getFloorX(), position.add(0, 1.5f, 0).getFloorY(), position.getFloorZ());
        BooleanSupplier cartStillHere = () -> this.getLevel() == level && this.getPosition().equals(position);
        boolean hasSource = false;

        if (level.getLoadedBlockEntity(above) instanceof AutomatedBlockStorage storage && storage instanceof SlotGroup source && !storage.isClosed()) {
            hasSource = true;
            if (this.pullItem(source, storage::blocksExtractionBelow, () -> cartStillHere.getAsBoolean() && !storage.isClosed() && level.getLoadedBlockEntity(above) == storage, events)) {
                return;
            }
        } else {
            BoundingBox sourceArea = new BoundingBox(position.add(-0.5f, 1, -0.5f), position.add(0.5f, 2, 0.5f));
            for (Entity entity : level.getCollidingEntities(sourceArea)) {
                if (entity == this || entity.isClosed() || !entity.isAlive()) {
                    continue;
                }

                StorageView source = switch (entity) {
                    case ContainerMinecart cart -> cart.getInventory();
                    case ChestBoat boat -> boat.getInventory();
                    default -> null;
                };

                if (source != null) {
                    hasSource = true;
                    if (this.pullItem(source, slot -> false,
                            () -> cartStillHere.getAsBoolean() && !entity.isClosed() && entity.isAlive()
                                    && entity.getLevel() == level && entity.getBoundingBox().intersects(sourceArea), events)) {
                        return;
                    }
                    break;
                }
            }
        }

        if (!this.canCollect() || !cartStillHere.getAsBoolean()) {
            return;
        }

        BoundingBox aboveArea = new BoundingBox(position.add(-0.5f, 11f / 16f, -0.5f), position.add(0.5f, 2, 0.5f));
        Set<Entity> searched = hasSource ? Set.of() : level.getCollidingEntities(aboveArea);
        for (Entity entity : searched) {
            if (!cartStillHere.getAsBoolean()) {
                return;
            }

            if (entity instanceof DroppedItem item && this.pickUpItem(item, aboveArea, events)) {
                return;
            }
        }

        if (!this.canCollect() || !cartStillHere.getAsBoolean()) {
            return;
        }

        BoundingBox sideArea = this.getBoundingBox().inflate(0.25f, 0, 0.25f);
        for (Entity entity : level.getCollidingEntities(sideArea)) {
            if (!cartStillHere.getAsBoolean()) {
                return;
            }

            if (!searched.contains(entity) && entity instanceof DroppedItem item && this.pickUpItem(item, sideArea, events)) {
                return;
            }
        }
    }

    private boolean pullItem(SlotGroup source, IntPredicate blocksExtraction, BooleanSupplier sourceAvailable, EventManager events) {
        StorageView target = this.getInventory();
        for (int slot = 0; slot < source.size(); slot++) {
            if (!this.canCollect() || !sourceAvailable.getAsBoolean()) {
                return false;
            }

            ItemStack original = source.getItem(slot);
            if (original.isEmpty() || blocksExtraction.test(slot)) {
                continue;
            }

            ItemStack offered = original.withCount(1);
            if (!this.inventoryContainer().canAddItem(offered)) {
                continue;
            }

            InventoryMoveItemEvent event = InventoryMoveItemEvent.transfer(source, target, target, offered);
            events.fire(event);
            ItemStack item = event.getItem();
            if (event.isCancelled() || !this.canCollect() || !sourceAvailable.getAsBoolean() || blocksExtraction.test(slot) || !original.equals(source.getItem(slot))) {
                return false;
            }

            if (!item.isEmpty() && item.getCount() == 1 && this.inventoryContainer().addItem(item).length == 0) {
                source.setItem(slot, original.decreaseCount());
                return true;
            }
        }

        return false;
    }

    private boolean pickUpItem(DroppedItem entity, BoundingBox area, EventManager events) {
        CloudLevel level = this.getLevel();
        Vector3f position = this.getPosition();
        if (!this.canCollect() || !isInPickupArea(entity, level, area)) {
            return false;
        }

        ItemStack original = entity.getItem();
        if (original.isEmpty() || this.inventoryContainer().getFreeSpace(original) == 0) {
            return false;
        }

        StorageView target = this.getInventory();
        InventoryMoveItemEvent event = InventoryMoveItemEvent.transferWithWorld(null, target, target, original);
        events.fire(event);
        ItemStack item = event.getItem();
        if (event.isCancelled() || !this.canCollect() || this.getLevel() != level || !this.getPosition().equals(position)
                || !isInPickupArea(entity, level, area) || !original.equals(entity.getItem())
                || item.isEmpty() || item.getCount() > original.getCount()) {
            return false;
        }

        ItemStack[] leftover = this.inventoryContainer().addItem(item);
        int accepted = item.getCount() - (leftover.length == 0 ? 0 : leftover[0].getCount());
        if (accepted == 0) {
            return false;
        }

        if (accepted == original.getCount()) {
            CloudEntity.remove(entity, EntityRemoveCause.PICKUP);
        } else {
            entity.setItem(original.decreaseCount(accepted));
        }

        return true;
    }

    private static boolean isInPickupArea(DroppedItem entity, CloudLevel level, BoundingBox area) {
        return !entity.isClosed() && entity.isAlive() && entity.getLevel() == level && entity.getBoundingBox().intersects(area);
    }
}
