package org.cloudburstmc.server.player.manager;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.event.inventory.InventoryCloseEvent;
import org.cloudburstmc.api.event.inventory.InventoryOpenEvent;
import org.cloudburstmc.api.inventory.VirtualContainerScreen;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerId;
import org.cloudburstmc.protocol.bedrock.packet.ContainerClosePacket;
import org.cloudburstmc.server.container.Container;
import org.cloudburstmc.server.container.screen.CloudHudScreen;
import org.cloudburstmc.server.container.screen.CloudInventoryScreen;
import org.cloudburstmc.server.player.CloudPlayer;

import java.util.Objects;
import java.util.StringJoiner;

/**
 * Owns the player's active screen and its network window. The HUD remains underneath it.
 */
@Log4j2
@Getter
public class PlayerInventoryManager {
    private final CloudPlayer player;
    private final CloudHudScreen defaultScreen;
    private CloudInventoryScreen currentScreen;
    private boolean openingScreen;
    private @Nullable Byte closingWindowId;
    private @Nullable CloudInventoryScreen pendingScreen;

    public PlayerInventoryManager(CloudPlayer player) {
        this.player = player;
        this.defaultScreen = new CloudHudScreen(player);
        this.defaultScreen.setup();
        this.player.getItemStackNetManager().pushScreen(this.defaultScreen);
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
        this.player.getItemStackNetManager().popScreen();
        screen.closeWindow(request);

        try {
            screen.close();
        } finally {
            this.player.getServer().getEventManager().fire(new InventoryCloseEvent(screen, reason));
        }
    }

    public void expectWindowClose(byte windowId) {
        this.closingWindowId = windowId;
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

        CloudInventoryScreen screen = this.currentScreen;
        if (screen != null && (screen.hasWindow(packet.getId()) || packet.getId() == ContainerId.NONE)) {
            boolean rejected = packet.getId() == ContainerId.NONE;
            this.closeScreen(rejected ? InventoryCloseEvent.Reason.CANT_USE : InventoryCloseEvent.Reason.PLAYER, packet);
        } else {
            this.player.sendPacket(packet);
        }
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
            this.player.getItemStackNetManager().pushScreen(screen);

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

    @NonNull
    public CloudInventoryScreen getScreen() {
        return this.player.getItemStackNetManager().getScreen();
    }

    public CloudInventoryScreen getOpenContainer() {
        return this.currentScreen;
    }

    public void sendAllInventories() {
        for (Container inv : this.player.getItemStackNetManager().getAllInventories()) {
            this.player.onInventoryContentsChange(inv);
        }
    }

    @Override
    public String toString() {
        StringJoiner str = new StringJoiner("\n");
        str.add("Player Inventories [" + player.getName() + ":");
        str.add("Main Inventory: ");
        for (int i = 0; i < player.getInventory().size(); i++) {
            str.add("  " + i + ": " + player.getInventory().getItem(i));
        }
        str.add("===============");
        str.add("EnderChest Inventory:");
        for (int i = 0; i < player.getEnderChest().size(); i++) {
            str.add("  " + i + ": " + player.getEnderChest().getItem(i));
        }

        return str.toString();
    }
}
