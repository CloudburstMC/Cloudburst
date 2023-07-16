package org.cloudburstmc.server.player.manager;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.cloudburstmc.api.container.Container;
import org.cloudburstmc.server.container.screen.CloudContainerScreen;
import org.cloudburstmc.server.container.screen.CloudHudScreen;
import org.cloudburstmc.server.player.CloudPlayer;

import java.util.Objects;
import java.util.StringJoiner;

@Log4j2
@Getter
public class PlayerInventoryManager {
    private final CloudPlayer player;
    private final CloudHudScreen defaultScreen;
    private CloudContainerScreen currentScreen;

    public PlayerInventoryManager(CloudPlayer player) {
        this.player = player;
        // This screen is always exists regardless of whether a player has something else on top.
        this.defaultScreen = new CloudHudScreen(player);
        this.player.getItemStackNetManager().pushScreen(this.defaultScreen);
    }

    public CloudContainerScreen closeScreen() {
        if (currentScreen != null) {
            CloudContainerScreen screen = this.player.getItemStackNetManager().popScreen();
            currentScreen.close();
            currentScreen = null;
            return screen;
        } else {
            return null;
        }
    }

    public void openScreen(@NonNull CloudContainerScreen screen) {
        Objects.requireNonNull(screen, "screen");
        if (this.currentScreen != null) {
            this.currentScreen.close();
            this.player.getItemStackNetManager().popScreen();
        }
        this.currentScreen = screen;
        this.currentScreen.setup();
        this.player.getItemStackNetManager().pushScreen(screen);
    }

    @NonNull
    public CloudContainerScreen getScreen() {
        return this.player.getItemStackNetManager().getScreen();
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
