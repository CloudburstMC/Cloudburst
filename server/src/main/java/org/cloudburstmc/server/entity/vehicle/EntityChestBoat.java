package org.cloudburstmc.server.entity.vehicle;

import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.vehicle.ChestBoat;
import org.cloudburstmc.api.event.entity.EntityRemoveCause;
import org.cloudburstmc.api.inventory.view.StorageView;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemType;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerType;
import org.cloudburstmc.server.entity.component.EntityStorage;
import org.cloudburstmc.server.entity.data.CloudChestBoatData;
import org.cloudburstmc.server.player.CloudPlayer;

import static org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes.*;

public class EntityChestBoat extends EntityBoat implements ChestBoat {

    private static final int INVENTORY_SIZE = 27;

    private final CloudChestBoatData persistenceState;
    private final EntityStorage storage;

    public EntityChestBoat(EntityType<ChestBoat> type, Location location) {
        CloudChestBoatData state = new CloudChestBoatData();
        super(type, location, state);
        this.persistenceState = state;
        this.storage = new EntityStorage(this, ContainerType.CONTAINER, INVENTORY_SIZE, true);
        state.setItems(this.storage.getInventory().getContainer());
        this.data.set(CONTAINER_TYPE, (byte) ContainerType.CHEST_BOAT.getId());
        this.data.set(CONTAINER_SIZE, INVENTORY_SIZE);
        this.data.set(CONTAINER_STRENGTH_MODIFIER, 0);
    }

    @Override
    protected CloudChestBoatData data() {
        return this.persistenceState;
    }

    @Override
    protected void loadCustom(NbtMap tag) {
        if (!tag.containsKey("Items")) {
            this.storage.getInventory().clear();
        }
        super.loadCustom(tag);
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
        return this.storage.getInventory();
    }

    public boolean openStorage(CloudPlayer player) {
        return this.storage.open(player);
    }

    @Override
    protected boolean tickEntity(int currentTick, int elapsedTicks) {
        boolean active = super.tickEntity(currentTick, elapsedTicks);
        this.storage.closeUnavailableViewers();
        return active;
    }

    @Override
    protected boolean interactDefault(Player player, ItemStack item, Vector3f clickedPosition) {
        if (player.isSpectator() || player.isSneaking() || !this.canAddPassenger(player)) {
            return player instanceof CloudPlayer cloudPlayer && this.openStorage(cloudPlayer);
        }
        return super.interactDefault(player, item, clickedPosition);
    }

    @Override
    protected void dropContents() {
        this.storage.dropContents();
    }

    @Override
    public void remove(EntityRemoveCause cause) {
        if (!this.isClosed()) {
            super.remove(cause);
            this.storage.close();
        }
    }
}
