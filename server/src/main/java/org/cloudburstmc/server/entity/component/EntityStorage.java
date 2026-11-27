package org.cloudburstmc.server.entity.component;

import lombok.Getter;
import org.cloudburstmc.api.event.inventory.InventoryCloseReason;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.level.sound.SoundPlayback;
import org.cloudburstmc.api.level.sound.SoundTypes;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerType;
import org.cloudburstmc.server.container.CloudContainer;
import org.cloudburstmc.server.container.screen.CloudEntityStorageScreen;
import org.cloudburstmc.server.container.view.CloudEntityStorageView;
import org.cloudburstmc.server.entity.CloudEntity;
import org.cloudburstmc.server.level.chunk.CloudChunk;
import org.cloudburstmc.server.player.CloudPlayer;

import java.util.HashSet;
import java.util.Set;

public class EntityStorage {

    private static final SoundPlayback CHEST_OPEN = new SoundPlayback(SoundTypes.RANDOM_CHESTOPEN, 0.5f, 1);
    private static final SoundPlayback CHEST_CLOSE = new SoundPlayback(SoundTypes.RANDOM_CHESTCLOSED, 0.5f, 1);
    private static final double MAX_ACCESS_DISTANCE_SQUARED = 64;

    @Getter
    private final CloudEntity entity;
    @Getter
    private final ContainerType windowType;
    private final boolean chestSounds;
    @Getter
    private final CloudEntityStorageView inventory;
    private final Set<CloudPlayer> viewers = new HashSet<>();

    public EntityStorage(CloudEntity entity, ContainerType windowType, int size, boolean chestSounds) {
        this.entity = entity;
        this.windowType = windowType;
        this.chestSounds = chestSounds;
        this.inventory = new CloudEntityStorageView(new CloudContainer(size) {
            @Override
            protected void onSlotChange(int slot, ItemStack previousItem) {
                EntityStorage.this.markDirty();
                super.onSlotChange(slot, previousItem);
            }

            @Override
            protected void onContentsChange() {
                EntityStorage.this.markDirty();
                super.onContentsChange();
            }
        });
    }

    public boolean isUnavailableTo(CloudPlayer player) {
        if (!this.entity.isValid() || !player.isAlive() || this.entity.getLevel() != player.getLevel()) {
            return true;
        }

        double distanceSquared = player.getPosition().distanceSquared(this.entity.getPosition());
        return Double.compare(distanceSquared, MAX_ACCESS_DISTANCE_SQUARED) > 0;
    }

    public boolean open(CloudPlayer player) {
        if (this.isUnavailableTo(player)) {
            return false;
        }

        if (!(player.getOpenInventory() instanceof CloudEntityStorageScreen screen) || screen.getEntity() != this.entity) {
            player.openInventory(new CloudEntityStorageScreen(player, this));
        }

        return true;
    }

    public void addViewer(CloudPlayer player) {
        if (this.viewers.add(player)) {
            this.inventory.getContainer().addContainerListener(player);
            if (this.chestSounds && this.viewers.size() == 1) {
                this.entity.getLevel().playSound(this.entity.getPosition(), CHEST_OPEN);
            }
        }
    }

    public void removeViewer(CloudPlayer player) {
        if (this.viewers.remove(player)) {
            this.inventory.getContainer().removeContainerListener(player);
            if (this.chestSounds && this.viewers.isEmpty() && !this.entity.isClosed()) {
                this.entity.getLevel().playSound(this.entity.getPosition(), CHEST_CLOSE);
            }
        }
    }

    public void closeUnavailableViewers() {
        for (CloudPlayer player : Set.copyOf(this.viewers)) {
            if (this.isUnavailableTo(player)) {
                player.closeInventory(InventoryCloseReason.CANT_USE);
            }
        }
    }

    public void dropContents() {
        CloudContainer container = this.inventory.getContainer();
        ItemStack[] items = container.getContents();
        container.clear();
        for (ItemStack item : items) {
            if (!item.isEmpty()) {
                this.entity.getLevel().dropItem(this.entity.getPosition(), item);
            }
        }
    }

    public void close() {
        for (CloudPlayer player : Set.copyOf(this.viewers)) {
            player.closeInventory(InventoryCloseReason.CANT_USE);
        }
        this.inventory.getContainer().close();
    }

    private void markDirty() {
        CloudChunk chunk = this.entity.getChunk();
        if (this.entity.isValid() && chunk != null) {
            chunk.markDirty();
        }
    }
}
