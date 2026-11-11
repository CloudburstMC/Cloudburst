package org.cloudburstmc.server.player.manager;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.event.inventory.InventoryCloseEvent;
import org.cloudburstmc.api.event.inventory.InventoryOpenEvent;
import org.cloudburstmc.api.inventory.VirtualContainerScreen;
import org.cloudburstmc.api.inventory.view.SlotGroup;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerId;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerType;
import org.cloudburstmc.protocol.bedrock.packet.ContainerClosePacket;
import org.cloudburstmc.protocol.bedrock.packet.ContainerOpenPacket;
import org.cloudburstmc.server.blockentity.ContainerBlockEntity;
import org.cloudburstmc.server.container.Container;
import org.cloudburstmc.server.container.screen.CloudHudScreen;
import org.cloudburstmc.server.container.screen.CloudInventoryScreen;
import org.cloudburstmc.server.container.view.CloudSlotGroupBase;
import org.cloudburstmc.server.player.CloudPlayer;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Owns the player's active screen and its network window. The HUD remains underneath it.
 */
@Log4j2
public class PlayerInventoryManager {

    private final CloudPlayer player;
    @Getter
    private final CloudHudScreen defaultScreen;
    private @Nullable CloudInventoryScreen currentScreen;
    private @Nullable Window window;
    private byte nextWindowId = 1;
    private boolean openingScreen;
    private @Nullable Byte closingWindowId;
    private @Nullable CloudInventoryScreen pendingScreen;

    public PlayerInventoryManager(CloudPlayer player) {
        this.player = player;
        this.defaultScreen = new CloudHudScreen(player);
        this.defaultScreen.setup();
    }

    @NonNull
    public CloudInventoryScreen getScreen() {
        return this.currentScreen == null ? this.defaultScreen : this.currentScreen;
    }

    public @Nullable CloudInventoryScreen getOpenContainer() {
        return this.currentScreen;
    }

    public void openScreen(@NonNull CloudInventoryScreen screen) {
        Objects.requireNonNull(screen, "screen");
        if (screen.getPlayer() != this.player) {
            throw new IllegalArgumentException("Screen belongs to another player");
        }

        if (this.openingScreen || this.currentScreen == screen) {
            return;
        }

        this.openingScreen = true;
        try {
            this.closeScreen(InventoryCloseEvent.Reason.OPEN_NEW);
            if (this.closingWindowId != null) {
                this.pendingScreen = screen;
                return;
            }

            InventoryOpenEvent event = new InventoryOpenEvent(screen);
            this.player.getServer().getEventManager().fire(event);
            if (event.isCancelled() || !this.player.isConnected()) {
                return;
            }

            if (event.getTitleOverride() != null) {
                if (screen instanceof VirtualContainerScreen virtual) {
                    virtual.setTitle(event.getTitleOverride());
                } else {
                    screen.setTitleOverride(event.getTitleOverride());
                }
            }

            screen.setup();
            this.currentScreen = screen;

            try {
                screen.open();
            } catch (Exception e) {
                try {
                    if (this.currentScreen == screen) {
                        this.closeScreen(InventoryCloseEvent.Reason.CANT_USE);
                    }
                } catch (Exception cleanupFailure) {
                    e.addSuppressed(cleanupFailure);
                }

                log.error("Failed to open screen {} for player {}", screen.getClass().getSimpleName(), player.getName(), e);
            }
        } finally {
            this.openingScreen = false;
        }
    }

    public void closeScreen(InventoryCloseEvent.Reason reason) {
        this.pendingScreen = null;
        this.closeScreen(reason, null);
    }

    private void closeScreen(InventoryCloseEvent.Reason reason, @Nullable ContainerClosePacket request) {
        CloudInventoryScreen screen = this.currentScreen;
        if (screen == null) {
            return;
        }

        this.currentScreen = null;
        try {
            this.closeWindow(request);
        } finally {
            try {
                screen.stopObservingContainers();
            } finally {
                try {
                    screen.close();
                } finally {
                    this.player.getServer().getEventManager().fire(new InventoryCloseEvent(screen, reason));
                }
            }
        }
    }

    public void openWindow(CloudInventoryScreen screen, ContainerOpenPacket packet, @Nullable Container storage) {
        if (this.currentScreen != screen || this.window != null) {
            throw new IllegalStateException("Only the active screen can open its window");
        }

        ContainerType type = Objects.requireNonNull(packet.getType(), "container type");
        byte id = (byte) ContainerId.INVENTORY;
        if (type != ContainerType.INVENTORY) {
            id = this.nextWindowId;
            this.nextWindowId = (byte) (id == 99 ? 1 : id + 1);
        }

        this.window = new Window(id, type, storage);
        packet.setId(id);
        this.player.sendPacket(packet);
    }

    public int getWindowId(Container container) {
        Window active = this.window;
        return active != null && active.storage() == container ? active.id() : ContainerId.NONE;
    }

    public void handleWindowClose(ContainerClosePacket packet) {
        if (this.closingWindowId != null && this.closingWindowId == packet.getId()) {
            this.closingWindowId = null;
            this.player.sendPacket(packet);

            CloudInventoryScreen pending = this.pendingScreen;
            this.pendingScreen = null;
            if (pending != null) {
                this.openScreen(pending);
            }

            return;
        }

        Window active = this.window;
        if (active != null && (active.id() == packet.getId() || packet.getId() == ContainerId.NONE)) {
            boolean rejected = packet.getId() == ContainerId.NONE;
            this.closeScreen(rejected ? InventoryCloseEvent.Reason.CANT_USE : InventoryCloseEvent.Reason.PLAYER, packet);
        } else {
            this.player.sendPacket(packet);
        }
    }

    private void closeWindow(@Nullable ContainerClosePacket request) {
        Window active = this.window;
        this.window = null;
        if (active == null || active.type() == ContainerType.LECTERN) {
            return;
        }

        boolean serverInitiated = request == null;
        if (serverInitiated) {
            this.closingWindowId = active.id();
        }

        ContainerClosePacket packet = new ContainerClosePacket();
        packet.setId(serverInitiated ? active.id() : request.getId());
        packet.setType(serverInitiated ? active.type() : Objects.requireNonNullElse(request.getType(), ContainerType.NONE));
        packet.setServerInitiated(serverInitiated);
        this.player.sendPacket(packet);
    }

    public Set<Container> getAllInventories() {
        Set<Container> inventories = new LinkedHashSet<>();
        collectInventories(this.defaultScreen, inventories);
        if (this.currentScreen != null) {
            collectInventories(this.currentScreen, inventories);
        }
        return inventories;
    }

    public void sendAllInventories() {
        for (Container inv : this.getAllInventories()) {
            this.player.onInventoryContentsChange(inv);
        }
    }

    private static void collectInventories(CloudInventoryScreen screen, Set<Container> inventories) {
        for (SlotGroup view : screen.getAllSlotGroups()) {
            if (view instanceof CloudSlotGroupBase section) {
                inventories.add(section.getContainer());
            } else if (view instanceof ContainerBlockEntity blockEntity) {
                inventories.add(blockEntity.getContainer());
            }
        }
    }

    private record Window(byte id, ContainerType type, @Nullable Container storage) {
    }
}
