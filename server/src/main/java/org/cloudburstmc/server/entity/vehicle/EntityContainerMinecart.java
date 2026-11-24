package org.cloudburstmc.server.entity.vehicle;

import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.vehicle.ContainerMinecart;
import org.cloudburstmc.api.event.entity.EntityRemoveCause;
import org.cloudburstmc.api.inventory.view.StorageView;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerType;
import org.cloudburstmc.server.container.CloudContainer;
import org.cloudburstmc.server.entity.component.EntityStorage;
import org.cloudburstmc.server.entity.data.CloudContainerMinecartData;
import org.cloudburstmc.server.player.CloudPlayer;

import static org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes.*;

public abstract class EntityContainerMinecart extends EntityAbstractMinecart implements ContainerMinecart {

    private final EntityStorage storage;

    protected EntityContainerMinecart(EntityType<? extends ContainerMinecart> type, Location location, int slots, ContainerType containerType, ContainerType windowType) {
        this(type, location, slots, containerType, windowType, new CloudContainerMinecartData());
    }

    protected EntityContainerMinecart(EntityType<? extends ContainerMinecart> type, Location location, int slots, ContainerType containerType, ContainerType windowType, CloudContainerMinecartData state) {
        super(type, location, state);
        this.storage = new EntityStorage(this, windowType, slots, false);
        state.setItems(this.storage.getInventory().getContainer());
        this.data.set(CONTAINER_TYPE, (byte) containerType.getId());
        this.data.set(CONTAINER_SIZE, slots);
        this.data.set(CONTAINER_STRENGTH_MODIFIER, 0);
    }

    @Override
    protected void loadCustom(NbtMap tag) {
        super.loadCustom(tag);
        if (!tag.containsKey("Items")) {
            this.storage.getInventory().clear();
        }
    }

    @Override
    public final StorageView getInventory() {
        return this.storage.getInventory();
    }

    protected final CloudContainer inventoryContainer() {
        return this.storage.getInventory().getContainer();
    }

    @Override
    public final boolean isRideable() {
        return false;
    }

    @Override
    public String getInteractionPrompt() {
        return this.isValid() ? "action.interact.opencontainer" : "";
    }

    @Override
    protected boolean interactDefault(Player player, ItemStack item, Vector3f clickedPosition) {
        return player instanceof CloudPlayer cloudPlayer && this.storage.open(cloudPlayer);
    }

    @Override
    protected boolean tickEntity(int currentTick, int elapsedTicks) {
        boolean active = super.tickEntity(currentTick, elapsedTicks);
        this.storage.closeUnavailableViewers();
        return active;
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
