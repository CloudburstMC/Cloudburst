package org.cloudburstmc.api.player;

import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.Server;
import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.blockentity.BlockEntity;
import org.cloudburstmc.api.entity.Creature;
import org.cloudburstmc.api.event.player.PlayerKickEvent;
import org.cloudburstmc.api.event.player.PlayerSetSpawnEvent;
import org.cloudburstmc.api.inventory.*;
import org.cloudburstmc.api.inventory.view.*;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.player.skin.Skin;
import org.cloudburstmc.api.util.data.CardinalDirection;
import org.cloudburstmc.math.vector.Vector3i;

import java.util.OptionalLong;
import java.util.UUID;

/**
 * Represents a connected player. Extends {@link Creature} with player-specific capabilities including
 * inventory management, game mode control, permissions, ban/whitelist handling, and screen interactions.
 */
public interface Player extends Creature {
    /**
     * Checks if this player is currently online.
     *
     * @return true if they are online
     */
    boolean isOnline();

    /**
     * Returns the name of this player.
     *
     * @return Player name
     */
    String getName();

    UUID getServerId();

    /**
     * Checks if this player is banned.
     *
     * @return true if banned
     */
    boolean isBanned();

    /**
     * Sets this player to be banned or to be pardoned.
     *
     * @param value true if banned
     */
    void setBanned(boolean value);

    /**
     * Checks if this player is whitelisted.
     *
     * @return true if whitelisted
     */
    boolean isWhitelisted();

    /**
     * Adds or removes this player from the whitelist.
     *
     * @param value true if whitelisted
     */
    void setWhitelisted(boolean value);

    /**
     * Returns the {@link Server} object carrying this player.
     *
     * @return The server instance
     */
    Server getServer();

    /**
     * Returns the time this player first played on this server.
     * <p>
     * If the player has never played before, this will return 0.
     *
     * @return An optional with the time this player first played on this server
     */
    OptionalLong getFirstPlayed();

    /**
     * Returns the time this player last joined in this server.
     * <p>
     * If the player has never played before, this will return 0.
     *
     * @return An optional with the time this player last played on this server
     */
    OptionalLong getLastPlayed();

    /**
     * Checks if the player has played on this server before.
     *
     * @return true if this player has played before
     */
    boolean hasPlayedBefore();

    boolean isInsideOfWater();

    boolean isSneaking();

    boolean isSleeping();

    boolean sleepOn(Vector3i pos);

    void stopSleep();

    boolean isOnGround();

    /**
     * Returns the player's main 36-slot inventory.
     *
     * <p><strong>This does NOT include:</strong></p>
     * <ul>
     *   <li>Armor slots: use {@link #getArmor()}</li>
     *   <li>Offhand slot: use {@link #getOffhand()}</li>
     *   <li>Crafting grid (when the player's inventory screen is open): use
     *       {@link #getInventoryScreen()} and access its crafting slot group</li>
     * </ul>
     *
     * <p>This method always returns the same object regardless of whether the player currently
     * has any screen open. For the full player-inventory screen (including crafting grid, armor,
     * and offhand), use {@link #getInventoryScreen()}.</p>
     *
     * <p><strong>Identity guarantee:</strong> the returned object is the same instance as
     * {@link org.cloudburstmc.api.inventory.ContainerScreen#getPlayerInventory()} for any
     * container screen the player currently has open.</p>
     *
     * @return the player's 36-slot main inventory
     */
    PlayerInventoryView getInventory();

    /**
     * Returns the player's hotbar, a 9-slot section view (slots 0-8) that mirrors the
     * first 9 slots of {@link #getInventory()}.
     *
     * <p>Use this when you need hotbar-specific semantics (selected slot, selected item)
     * without operating on the full 36-slot inventory.</p>
     *
     * @return the hotbar section
     */
    HotbarView getHotbar();

    /**
     * Returns the player's armor equipment slots (helmet, chestplate, leggings, boots).
     *
     * @return the armor slot group
     */
    ArmorView getArmor();

    /**
     * Returns the player's offhand slot.
     *
     * @return the offhand slot group
     */
    OffhandView getOffhand();

    /**
     * Returns the player's ender chest storage (27 slots).
     *
     * @return the ender chest slot group
     */
    EnderChestView getEnderChest();

    boolean isOp();

    void setOp(boolean value);

    float getMovementSpeed();

    void setMovementSpeed(float speed);

    Level getLevel();

    boolean isSpectator();

    boolean isCreative();

    boolean isSurvival();

    void resetInAirTicks();

    boolean isSpawned();

    GameMode getGamemode();

    /**
     * Returns the display name of this player.
     *
     * @return the display name component
     */
    Component displayName();

    /**
     * Sets the display name of this player.
     *
     * @param displayName the new display name component
     */
    void displayName(Component displayName);

    String getXuid();

    boolean isConnected();

    /**
     * Kicks this player with the given reason displayed on their screen.
     *
     * @param reason the disconnect message shown to the player
     */
    void kick(Component reason);

    /**
     * Kicks this player with the given reason and kick-event cause.
     *
     * @param reason the disconnect message shown to the player
     * @param cause  the kick cause reported to {@link org.cloudburstmc.api.event.player.PlayerKickEvent}
     */
    void kick(Component reason, PlayerKickEvent.Reason cause);

    /**
     * Returns the current round-trip latency of this player's connection in milliseconds.
     *
     * @return ping in milliseconds, or 0 if the connection is not yet established
     */
    int getPing();

    default void save() {
        save(false);
    }

    void save(boolean async);

    CardinalDirection getCardinalDirection();

    boolean isAdventure();

    Location getSpawn();

    /**
     * Sets the player's personal spawn point.
     *
     * @param spawn the new spawn location, or {@code null} to clear
     */
    void setSpawn(Location spawn);

    /**
     * Sets the player's personal spawn point with an explicit cause.
     *
     * @param spawn the new spawn location, or {@code null} to clear
     * @param cause the reason for the change (used as the event cause)
     */
    void setSpawn(@Nullable Location spawn, PlayerSetSpawnEvent.Cause cause);

    /**
     * Clears the player's personal spawn point so they will respawn at the world spawn.
     */
    void clearSpawn();

    Skin getSkin();

    void setSkin(Skin newSkin);

    /**
     * Returns the player's HUD screen (the always-visible hotbar and offhand slots).
     *
     * <p>This screen is open at all times, even when a container is also open.
     * Use it to access the hotbar and offhand slot groups without holding a reference
     * to an open container screen.</p>
     *
     * @return the player's HUD screen
     */
    HudScreen getHudScreen();

    /**
     * Returns the inventory screen currently open for this player,
     * or {@code null} if the player has no container open beyond the default HUD.
     *
     * <p>This method returns {@code null} whenever the player is looking at the normal
     * game view (i.e. only the HUD / hotbar is visible). It returns a non-{@code null}
     * value only while an explicit container screen is open (for example a chest, furnace,
     * crafting table, or a virtual inventory opened by a plugin.  The persistent
     * player-inventory screen ({@link ScreenTypes#INVENTORY}) and HUD screen
     * ({@link ScreenTypes#HUD}) are <em>not</em> returned by this method.</p>
     *
     * <p>To obtain the currently visible screen without null-checking, use
     * {@link #getCurrentScreen()} instead.</p>
     *
     * @return the currently open {@link InventoryScreen}, or {@code null}
     */
    @Nullable
    InventoryScreen getOpenInventory();

    /**
     * Returns the screen the player is currently viewing, never {@code null}.
     *
     * <p>Specifically:</p>
     * <ul>
     *   <li>If a container screen is open (chest, furnace, etc.), returns that {@link ContainerScreen}.</li>
     *   <li>If the player's own inventory is open (2×2 crafting grid / armor / offhand), returns
     *       the {@link PlayerInventoryScreen}.</li>
     *   <li>Otherwise (normal game view, only the hotbar is visible), returns the
     *       {@link HudScreen}.</li>
     * </ul>
     *
     * @return the current screen, never {@code null}
     */
    InventoryScreen getCurrentScreen();

    /**
     * Returns the player's own inventory screen if the player currently has it open, or {@code null} otherwise.
     *
     * <p>This returns a non-null value only while the player is viewing their own inventory
     * (the 2×2 crafting grid, armor, offhand, and main inventory). While any other screen
     * ({@link ContainerScreen}, HUD, etc.) is active, this method returns {@code null}.</p>
     *
     * @return the {@link PlayerInventoryScreen} if the player's own inventory is open, or {@code null}
     */
    @Nullable
    PlayerInventoryScreen getInventoryScreen();

    /**
     * Returns the currently open inventory screen if it matches the given type, or {@code null} otherwise.
     *
     * <p>Equivalent to calling {@link #getOpenInventory()} and casting to {@code T} after a type check,
     * but without requiring the caller to perform the cast explicitly:</p>
     * <pre>{@code
     * StorageScreen chest = player.getOpenInventory(ScreenTypes.CHEST);
     * if (chest != null) { ... }
     * }</pre>
     *
     * @param type the screen type token to match
     * @param <T>  the screen interface type
     * @return the open screen cast to {@code T}, or {@code null} if no screen is open or it is a different type
     */
    @Nullable
    @SuppressWarnings("unchecked")
    default <T extends InventoryScreen> T getOpenInventory(ScreenType<T> type) {
        InventoryScreen screen = getOpenInventory();
        if (screen != null && screen.getType() == type) {
            return (T) screen;
        }
        return null;
    }

    /**
     * Opens the given inventory screen for this player. If the player already
     * has a screen open it will be closed first.
     *
     * @param view the screen to open
     */
    void openInventory(InventoryScreen view);

    /**
     * Closes the player's currently open inventory/container screen.
     * If the player has no screen open, this method does nothing.
     * This method is server-initiated and will notify the client to close the UI.
     */
    void closeInventory();

    /**
     * Opens the container associated with the given block for this player.
     * The block must be a container block (e.g. a chest, furnace, or hopper).
     * If the player already has a screen open it will be closed first.
     *
     * @param block the container block to open
     * @throws IllegalArgumentException if the block is not a container
     */
    void openContainer(Block block);

    /**
     * Opens the container associated with the given block entity for this player.
     * The block entity must be a container (e.g. a chest, furnace, or hopper block entity).
     * If the player already has a screen open it will be closed first.
     *
     * @param blockEntity the container block entity to open
     * @throws IllegalArgumentException if the block entity is not a container
     */
    void openContainer(BlockEntity blockEntity);

    /**
     * Creates a new virtual single-chest (27-slot) view for this player.
     *
     * <p>The screen is not opened automatically; call {@link #openInventory(InventoryScreen)} to show
     * it. A virtual inventory has no backing block in the world; the server temporarily injects a
     * phantom chest block above the player on the client side for the duration the screen is open.</p>
     *
     * @param title the title displayed in the GUI title bar
     * @return a new {@link VirtualChestScreen} bound to this player
     */
    VirtualChestScreen createVirtualChest(Component title);

    /**
     * Creates a new virtual double-chest (54-slot) screen for this player.
     *
     * <p>The screen is not opened automatically; call {@link #openInventory(InventoryScreen)} to show
     * it. A virtual inventory has no backing block in the world; the server temporarily injects two
     * adjacent phantom chest blocks above the player on the client side.</p>
     *
     * @param title the title displayed in the GUI title bar
     * @return a new {@link VirtualDoubleChestScreen} bound to this player
     */
    VirtualDoubleChestScreen createVirtualDoubleChest(Component title);

    /**
     * Creates a new virtual hopper (5-slot) screen for this player.
     *
     * <p>The screen is not opened automatically; call {@link #openInventory(InventoryScreen)} to show
     * it. A virtual inventory has no backing block in the world; the server temporarily injects a
     * phantom hopper block above the player on the client side for the duration the screen is open.</p>
     *
     * @param title the title displayed in the GUI title bar
     * @return a new {@link VirtualHopperScreen} bound to this player
     */
    VirtualHopperScreen createVirtualHopper(Component title);
}
