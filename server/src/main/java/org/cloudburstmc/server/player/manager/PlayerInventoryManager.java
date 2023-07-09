package org.cloudburstmc.server.player.manager;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.cloudburstmc.api.inventory.Inventory;
import org.cloudburstmc.server.inventory.CloudArmorInventory;
import org.cloudburstmc.server.inventory.CloudEnderChestInventory;
import org.cloudburstmc.server.inventory.CloudHandInventory;
import org.cloudburstmc.server.inventory.CloudPlayerInventory;
import org.cloudburstmc.server.inventory.screen.CloudInventoryScreen;
import org.cloudburstmc.server.inventory.screen.HudInventoryScreen;
import org.cloudburstmc.server.player.CloudPlayer;

import java.util.Objects;
import java.util.StringJoiner;

@Log4j2
@Getter
public class PlayerInventoryManager {
    private final CloudPlayer player;
    private final CloudPlayerInventory inventory;
    private final CloudArmorInventory armor;
    private final CloudHandInventory hand;
    private final CloudEnderChestInventory enderChest;
    private final HudInventoryScreen defaultScreen;
    private CloudInventoryScreen currentScreen;

    public PlayerInventoryManager(CloudPlayer player) {
        this.player = player;
        this.inventory = new CloudPlayerInventory(player);
        this.armor = new CloudArmorInventory(player);
        this.hand = new CloudHandInventory(player);
        this.enderChest = new CloudEnderChestInventory(player);
        // This screen is always exists regardless of whether a player has something else on top.
        this.defaultScreen = new HudInventoryScreen(player);
        this.player.getItemStackNetManager().pushScreen(this.defaultScreen);
    }

    public CloudInventoryScreen closeScreen() {
        if (currentScreen != null) {
            CloudInventoryScreen screen = this.player.getItemStackNetManager().popScreen();
            currentScreen.close();
            currentScreen = null;
            return screen;
        } else {
            return null;
        }
    }

    public void openScreen(@NonNull CloudInventoryScreen screen) {
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
    public CloudInventoryScreen getScreen() {
        return this.player.getItemStackNetManager().getScreen();
    }

    public void sendAllInventories() {
        for (Inventory inv : this.player.getItemStackNetManager().getAllInventories()) {
            // TODO: Resend contents of all inventories visible to the player
        }
    }

    @Override
    public String toString() {
        StringJoiner str = new StringJoiner("\n");
        str.add("Player Inventories [" + player.getName() + ":");
        str.add("Main Inventory: ");
        for (int i = 0; i < inventory.getSize(); i++) {
            str.add("  " + i + ": " + inventory.getItem(i));
        }
        str.add("===============");
        str.add("EnderChest Inventory:");
        for (int i = 0; i < enderChest.getSize(); i++) {
            str.add("  " + i + ": " + enderChest.getItem(i));
        }

        return str.toString();
    }
}
