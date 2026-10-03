package org.cloudburstmc.server.entity.vehicle;

import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.vehicle.ChestBoat;
import org.cloudburstmc.api.event.inventory.InventoryCloseEvent;
import org.cloudburstmc.api.inventory.view.StorageView;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemType;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.level.sound.SoundPlayback;
import org.cloudburstmc.api.level.sound.SoundTypes;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerType;
import org.cloudburstmc.server.container.CloudContainer;
import org.cloudburstmc.server.container.screen.CloudChestBoatScreen;
import org.cloudburstmc.server.container.view.CloudEntityStorageView;
import org.cloudburstmc.server.item.ItemUtils;
import org.cloudburstmc.server.player.CloudPlayer;

import java.util.HashSet;
import java.util.Set;

import static org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes.*;

public class EntityChestBoat extends EntityBoat implements ChestBoat {

    private final CloudContainer container = new CloudContainer(27);
    private final CloudEntityStorageView inventory = new CloudEntityStorageView(this.container);
    private final Set<CloudPlayer> inventoryViewers = new HashSet<>();

    public EntityChestBoat(EntityType<ChestBoat> type, Location location) {
        super(type, location);
        this.data.set(CONTAINER_TYPE, (byte) ContainerType.CHEST_BOAT.getId());
        this.data.set(CONTAINER_SIZE, 27);
        this.data.set(CONTAINER_STRENGTH_MODIFIER, 0);
    }

    @Override
    public void loadAdditionalData(NbtMap tag) {
        super.loadAdditionalData(tag);
        this.container.clear();
        tag.listenForList("Items", NbtType.COMPOUND, items -> {
            for (NbtMap item : items) {
                int slot = Byte.toUnsignedInt(item.getByte("Slot"));
                if (slot >= this.container.size()) {
                    throw new IllegalArgumentException("Invalid chest boat inventory slot " + slot);
                }

                this.container.setItem(slot, ItemUtils.deserializeItem(item));
            }
        });
    }

    @Override
    public void saveAdditionalData(NbtMapBuilder tag) {
        super.saveAdditionalData(tag);
        tag.putList("Items", NbtType.COMPOUND, this.container.toNbt());
    }

    @Override
    public ItemType getBoatItem() {
        return VanillaBoats.definition(this.getBoatType()).chestItem();
    }

    @Override
    public int getMaxPassengers() {
        return 1;
    }

    @Override
    protected float getSinglePassengerOffset() {
        return 0.15f;
    }

    @Override
    public StorageView getInventory() {
        return this.inventory;
    }

    @Override
    public boolean onUpdate(int currentTick) {
        boolean active = super.onUpdate(currentTick);
        for (CloudPlayer player : Set.copyOf(this.inventoryViewers)) {
            if (this.isInventoryUnavailable(player)) {
                player.closeInventory(InventoryCloseEvent.Reason.CANT_USE);
            }
        }

        return active;
    }

    public boolean isInventoryUnavailable(CloudPlayer player) {
        float distanceSquared = player.getPosition().distanceSquared(this.position);
        return this.closed || !this.isAlive() || !player.isAlive() || player.getLevel() != this.level || !Float.isFinite(distanceSquared) || distanceSquared > 64;
    }

    public void openInventory(CloudPlayer player) {
        if (this.isInventoryUnavailable(player)) {
            return;
        }

        if (player.getOpenInventory() instanceof CloudChestBoatScreen screen && screen.getEntity() == this) {
            return;
        }

        player.openInventory(new CloudChestBoatScreen(player, this));
    }

    public void addInventoryViewer(CloudPlayer player) {
        if (this.inventoryViewers.add(player)) {
            this.container.addContainerListener(player);
            if (this.inventoryViewers.size() == 1) {
                this.level.playSound(this.position, new SoundPlayback(SoundTypes.RANDOM_CHESTOPEN, 0.5f, 1));
            }
        }
    }

    public void removeInventoryViewer(CloudPlayer player) {
        if (this.inventoryViewers.remove(player)) {
            this.container.removeContainerListener(player);
            if (this.inventoryViewers.isEmpty() && !this.closed) {
                this.level.playSound(this.position, new SoundPlayback(SoundTypes.RANDOM_CHESTCLOSED, 0.5f, 1));
            }
        }
    }

    @Override
    public boolean onInteract(Player player, ItemStack item, Vector3f clickedPos) {
        if (player.isSpectator() || player.isSneaking() || !this.canAddPassenger(player)) {
            this.openInventory((CloudPlayer) player);
            return true;
        }

        return super.onInteract(player, item, clickedPos);
    }

    @Override
    protected void dropContents() {
        for (ItemStack item : this.container.getContents()) {
            if (!item.isEmpty()) {
                this.level.dropItem(this.position, item);
            }
        }

        this.container.clear();
    }

    @Override
    public void close() {
        if (this.closed) {
            return;
        }

        super.close();

        for (CloudPlayer player : Set.copyOf(this.inventoryViewers)) {
            player.closeInventory(InventoryCloseEvent.Reason.CANT_USE);
        }

        this.container.close();
    }
}
