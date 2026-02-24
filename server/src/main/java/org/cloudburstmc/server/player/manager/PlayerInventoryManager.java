package org.cloudburstmc.server.player.manager;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.cloudburstmc.server.container.Container;
import org.cloudburstmc.api.event.inventory.InventoryCloseEvent;
import org.cloudburstmc.api.event.inventory.InventoryOpenEvent;
import org.cloudburstmc.api.inventory.VirtualContainerScreen;
import org.cloudburstmc.server.container.screen.CloudHudScreen;
import org.cloudburstmc.server.container.screen.CloudInventoryScreen;
import org.cloudburstmc.server.player.CloudPlayer;

import java.util.Objects;
import java.util.StringJoiner;

/**
 * Manages the stack of open {@link CloudInventoryScreen}s for a player. The {@link CloudHudScreen}
 * is always present as the base of the stack. Handles open and close transitions and fires
 * {@link org.cloudburstmc.api.event.inventory.InventoryOpenEvent} /
 * {@link org.cloudburstmc.api.event.inventory.InventoryCloseEvent} accordingly.
 */
@Log4j2
@Getter
public class PlayerInventoryManager {
    private final CloudPlayer player;
    private final CloudHudScreen defaultScreen;
    private CloudInventoryScreen currentScreen;

    public PlayerInventoryManager(CloudPlayer player) {
        this.player = player;
        // This screen is always exists regardless of whether a player has something else on top.
        this.defaultScreen = new CloudHudScreen(player);
        this.defaultScreen.setup();
        this.player.getItemStackNetManager().pushScreen(this.defaultScreen);
    }

    public CloudInventoryScreen closeScreen() {
        return closeScreen(InventoryCloseEvent.Reason.UNKNOWN);
    }

    public CloudInventoryScreen closeScreen(InventoryCloseEvent.Reason reason) {
        if (currentScreen != null) {
            CloudInventoryScreen screen = this.player.getItemStackNetManager().popScreen();
            currentScreen.close();
            currentScreen = null;
            InventoryCloseEvent event = new InventoryCloseEvent(screen, reason);
            player.getServer().getEventManager().fire(event);
            return screen;
        } else {
            return null;
        }
    }

    public void openScreen(@NonNull CloudInventoryScreen screen) {
        Objects.requireNonNull(screen, "screen");
        if (this.currentScreen != null) {
            closeScreen(InventoryCloseEvent.Reason.OPEN_NEW);
        }

        InventoryOpenEvent event = new InventoryOpenEvent(screen);
        player.getServer().getEventManager().fire(event);
        if (event.isCancelled()) {
            return;
        }
        if (event.getTitleOverride() != null) {
            if (screen instanceof VirtualContainerScreen virtual) {
                virtual.setTitle(event.getTitleOverride());
            } else {
                screen.setTitleOverride(event.getTitleOverride());
            }
        }

        try {
            screen.setup();
            screen.open();
        } catch (Exception e) {
            log.error("Failed to open screen {} for player {}", screen.getClass().getSimpleName(), player.getName(), e);
            return;
        }
        this.currentScreen = screen;
        this.player.getItemStackNetManager().pushScreen(screen);
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
