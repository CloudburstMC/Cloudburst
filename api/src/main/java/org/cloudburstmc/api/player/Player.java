package org.cloudburstmc.api.player;

import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.ban.BanEntry;
import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.blockentity.BlockEntity;
import org.cloudburstmc.api.command.CommandSender;
import org.cloudburstmc.api.entity.Creature;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.projectile.FishingHook;
import org.cloudburstmc.api.event.player.PlayerSetSpawnEvent;
import org.cloudburstmc.api.inventory.*;
import org.cloudburstmc.api.inventory.view.*;
import org.cloudburstmc.api.item.ItemType;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.level.sound.SoundPlayback;
import org.cloudburstmc.api.level.sound.SoundType;
import org.cloudburstmc.api.player.skin.Skin;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.api.util.data.CardinalDirection;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;

import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.Objects;
import java.util.OptionalLong;
import java.util.UUID;

/**
 * Represents a connected player. Extends {@link Creature} with player-specific capabilities including
 * inventory management, game mode control, permissions, ban/whitelist handling, and screen interactions.
 */
public interface Player extends Creature, CommandSender {

    /**
     * Checks if this player is currently online.
     *
     * @return true if they are online
     */
    boolean isOnline();

    /**
     * Returns the remote address of this connection, or null when unavailable.
     *
     * @return the remote IP address and port, or null
     */
    @Nullable
    InetSocketAddress getAddress();

    /**
     * Returns the identifier used for this player's current server connection.
     *
     * @return the server connection identifier
     */
    UUID getServerId();

    /**
     * Returns this player's connection profile.
     *
     * @return the player profile
     */
    PlayerProfile getProfile();

    /**
     * Returns the settings reported by this player's client.
     *
     * @return the client information
     */
    PlayerClientInfo getClientInfo();

    /**
     * Returns the authenticated Xbox user identifier, or an empty string when unauthenticated.
     */
    String getXuid();

    /**
     * Returns whether the player's network connection is active.
     */
    boolean isConnected();

    /**
     * Returns whether the player has completed their initial spawn.
     */
    boolean isSpawned();

    /**
     * Checks whether this player's UUID has an active ban.
     *
     * @return true if banned
     */
    boolean isBanned();

    /**
     * Adds or replaces this player's UUID ban and optionally disconnects them.
     *
     * @param reason the reason, or null for the default
     * @param expiration the expiration, or null for a permanent ban
     * @param source the issuer, or null for the default
     * @param kick whether to disconnect the player when the saved ban is active
     * @return the saved ban
     */
    BanEntry<UUID> ban(@Nullable String reason, @Nullable Instant expiration, @Nullable String source, boolean kick);

    /**
     * Checks whether this player's UUID is explicitly whitelisted.
     * Operators and players admitted while the whitelist is disabled are not implicitly listed.
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
     * Returns when this player first played on this server.
     *
     * @return milliseconds since the Unix epoch, or an empty optional if unknown
     */
    OptionalLong getFirstPlayed();

    /**
     * Returns when this player last joined this server.
     *
     * @return milliseconds since the Unix epoch, or an empty optional if unknown
     */
    OptionalLong getLastPlayed();

    /**
     * Checks if the player has played on this server before.
     *
     * @return true if this player has played before
     */
    boolean hasPlayedBefore();

    /**
     * Returns whether the player's eyes are below the surface of water.
     */
    boolean isInsideOfWater();

    /**
     * Reports the sneaking state independently of the resolved {@link #getPose() pose}.
     *
     * @return whether sneaking is active
     */
    boolean isSneaking();

    /**
     * Returns whether the player is sleeping in a bed.
     */
    boolean isSleeping();

    /**
     * Attempts to sleep in the bed at the supplied position in the player's level.
     *
     * @param position the bed position
     * @return whether the player started sleeping
     */
    boolean sleepOn(Vector3i position);

    /**
     * Wakes the player if they are sleeping.
     */
    void stopSleep();

    /**
     * Returns whether the player is supported by the ground.
     */
    boolean isOnGround();

    /**
     * Returns the player's main 36-slot inventory.
     *
     * <p>Armor, offhand, and crafting slots are separate. The same inventory is available
     * regardless of the open screen and through {@link ContainerScreen#getPlayerInventory()}.</p>
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

    /**
     * Returns the remaining cooldown for an item type.
     *
     * @param itemType the item type
     * @return remaining cooldown in ticks, or zero when the item is ready
     */
    int getItemCooldown(ItemType itemType);

    /**
     * Returns whether an item type is currently on cooldown.
     *
     * @param itemType the item type
     * @return whether the item is on cooldown
     */
    default boolean hasItemCooldown(ItemType itemType) {
        return this.getItemCooldown(itemType) > 0;
    }

    /**
     * Sets the cooldown shared by stacks of an item type.
     *
     * @param itemType the item type
     * @param ticks    the non-negative cooldown in ticks
     */
    void setItemCooldown(ItemType itemType, int ticks);

    /**
     * Returns this player's active fishing hook.
     *
     * @return the active hook, or {@code null} when the player is not fishing
     */
    @Nullable
    FishingHook getFishingHook();

    /**
     * Returns the player's movement-speed attribute.
     */
    float getMovementSpeed();

    /**
     * Sets the player's movement-speed attribute and updates their movement abilities.
     *
     * @param speed the new movement speed
     */
    void setMovementSpeed(float speed);

    /**
     * Returns the player's current abilities. Call {@link PlayerAbilities#update()} after
     * making changes for them to take effect.
     */
    PlayerAbilities getAbilities();

    /**
     * Returns the level the player is currently in.
     */
    Level getLevel();

    /**
     * Resets the elapsed time used to check how long the player has been airborne.
     */
    void resetInAirTicks();

    /**
     * Returns the player's current game mode.
     *
     * @return the current {@link GameMode}
     */
    GameMode getGameMode();

    /**
     * Sets the player's game mode.
     *
     * @param gameMode the new {@link GameMode}
     */
    void setGameMode(GameMode gameMode);

    /**
     * Returns the game mode this player was in before their current one, or
     * {@code null} if the game mode has never been changed since login.
     *
     * @return the previous {@link GameMode}, or {@code null}
     */
    @Nullable
    GameMode getPreviousGameMode();

    /**
     * Returns {@code true} if the player is currently in {@link GameMode#SURVIVAL}.
     */
    default boolean isSurvival() {
        return getGameMode() == GameMode.SURVIVAL;
    }

    /**
     * Returns {@code true} if the player is currently in {@link GameMode#CREATIVE}.
     */
    default boolean isCreative() {
        return getGameMode() == GameMode.CREATIVE;
    }

    /**
     * Returns {@code true} if the player is currently in {@link GameMode#ADVENTURE}.
     */
    default boolean isAdventure() {
        return getGameMode() == GameMode.ADVENTURE;
    }

    /**
     * Returns {@code true} if the player is currently in {@link GameMode#SPECTATOR}.
     */
    default boolean isSpectator() {
        return getGameMode() == GameMode.SPECTATOR;
    }

    /**
     * Returns the current food level, from zero to twenty.
     *
     * @return food level
     */
    int getFoodLevel();

    /**
     * Sets the food level, clamped to the range from 0 to 20.
     *
     * @param level food level
     */
    void setFoodLevel(int level);

    /**
     * Returns the saturation remaining before exhaustion starts reducing food.
     *
     * @return saturation level
     */
    float getSaturation();

    /**
     * Sets the saturation level, clamped to the range from 0 to 20.
     * Saturation may exceed the current food level.
     *
     * @param saturation saturation level
     */
    void setSaturation(float saturation);

    /**
     * Returns accumulated exhaustion.
     *
     * @return exhaustion level
     */
    float getExhaustion();

    /**
     * Adds exhaustion with the supplied cause, firing
     * {@link org.cloudburstmc.api.event.player.PlayerExhaustionEvent} before the increase.
     * A zero amount still fires the event so listeners may supply an increase.
     * Negative or non-finite amounts have no effect.
     *
     * @param amount exhaustion to add
     * @param reason the cause of the increase
     * @throws NullPointerException if the reason is {@code null}
     */
    void addExhaustion(float amount, ExhaustionReason reason);

    /**
     * Returns the player's experience level.
     *
     * @return the experience level
     */
    int getExperienceLevel();

    /**
     * Returns the display name of this player.
     *
     * @return the display name component
     */
    @Override
    Component displayName();

    /**
     * Sets the display name of this player.
     *
     * @param displayName the new display name component
     */
    void displayName(Component displayName);

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
    void kick(Component reason, KickReason cause);

    /**
     * Returns the current round-trip latency of this player's connection in milliseconds.
     *
     * @return ping in milliseconds, or 0 if the connection is not yet established
     */
    int getPing();

    /**
     * Plays a named sound at this player's position for this player only.
     *
     * @param sound  the sound to play
     * @param volume a finite, non-negative volume multiplier
     * @param pitch  a finite, positive pitch multiplier
     */
    default void playSound(SoundType sound, float volume, float pitch) {
        this.playSound(this.getPosition(), sound, volume, pitch);
    }

    /**
     * Plays a named sound for this player only. The resource-pack definition
     * determines its category and attenuation.
     *
     * @param position the finite sound origin
     * @param sound    the sound to play
     * @param volume   a finite, non-negative volume multiplier
     * @param pitch    a finite, positive pitch multiplier
     */
    default void playSound(Vector3f position, SoundType sound, float volume, float pitch) {
        this.playSound(position, new SoundPlayback(sound, volume, pitch));
    }

    /**
     * Plays a sound with the supplied settings at this player's current position.
     *
     * @param playback the sound and playback settings
     */
    default void playSound(SoundPlayback playback) {
        this.playSound(this.getPosition(), playback);
    }

    /**
     * Plays a sound with the supplied settings for this player only.
     *
     * @param position the finite sound origin
     * @param playback the sound and playback settings
     */
    void playSound(Vector3f position, SoundPlayback playback);

    /**
     * Plays a sound at this player's position. The resource-pack definition selects
     * the category and samples, so source and seed overrides are not applied.
     *
     * @param sound the sound to play
     */
    @Override
    default void playSound(net.kyori.adventure.sound.@NonNull Sound sound) {
        Objects.requireNonNull(sound, "sound");
        this.playSound(this.getPosition(),
                SoundType.of(Identifier.parse(sound.name().asString())),
                sound.volume(),
                sound.pitch());
    }

    /**
     * Plays a sound at a fixed position. Category and sample selection follow the
     * resource-pack definition, not the sound's source or seed overrides.
     *
     * @param sound the sound to play
     * @param x     the finite x coordinate
     * @param y     the finite y coordinate
     * @param z     the finite z coordinate
     */
    @Override
    default void playSound(net.kyori.adventure.sound.@NonNull Sound sound, double x, double y, double z) {
        Objects.requireNonNull(sound, "sound");
        this.playSound(Vector3f.from(x, y, z),
                SoundType.of(Identifier.parse(sound.name().asString())),
                sound.volume(),
                sound.pitch());
    }

    /**
     * Plays a sound at the emitter's current position. Playback is not attached
     * to subsequent movement. Category and sample selection follow the resource pack.
     *
     * @param sound   the sound to play
     * @param emitter an entity or {@link net.kyori.adventure.sound.Sound.Emitter#self()}
     * @throws IllegalArgumentException if the emitter is neither an entity nor self
     */
    @Override
    default void playSound(net.kyori.adventure.sound.@NonNull Sound sound,
                           net.kyori.adventure.sound.Sound.@NonNull Emitter emitter) {
        Objects.requireNonNull(emitter, "emitter");
        if (emitter == net.kyori.adventure.sound.Sound.Emitter.self()) {
            this.playSound(sound);
        } else if (emitter instanceof Entity entity) {
            Vector3f position = entity.getPosition();
            this.playSound(sound, position.getX(), position.getY(), position.getZ());
        } else {
            throw new IllegalArgumentException("Sound emitter must be an entity or self");
        }
    }

    /**
     * Stops all playing instances of the named sound for this player.
     *
     * @param sound the sound to stop
     */
    void stopSound(SoundType sound);

    /**
     * Stops all playing instances of the sound's identifier. Its source override
     * does not change the resource-pack category used during playback.
     *
     * @param sound the sound to stop
     */
    @Override
    default void stopSound(net.kyori.adventure.sound.@NonNull Sound sound) {
        Objects.requireNonNull(sound, "sound");
        this.stopSound(SoundType.of(Identifier.parse(sound.name().asString())));
    }

    /**
     * Stops a named sound or all sounds. Category-filtered stops are not supported.
     *
     * @param stop the sounds to stop
     * @throws IllegalArgumentException if a category filter is supplied
     */
    @Override
    default void stopSound(net.kyori.adventure.sound.@NonNull SoundStop stop) {
        Objects.requireNonNull(stop, "stop");
        if (stop.source() != null) {
            throw new IllegalArgumentException("Sound categories cannot be filtered when stopping sounds");
        }
        net.kyori.adventure.key.Key name = stop.sound();
        if (name == null) {
            this.stopAllSounds();
        } else {
            this.stopSound(SoundType.of(Identifier.parse(name.asString())));
        }
    }

    /**
     * Stops all sounds currently playing for this player.
     */
    void stopAllSounds();

    /**
     * Sends a popup message.
     *
     * @param message the popup message
     */
    void sendPopup(Component message);

    /**
     * Sends a tip message.
     *
     * @param message the tip message
     */
    void sendTip(Component message);

    /**
     * Sends an action bar message.
     *
     * @param message the action bar message
     */
    @Override
    void sendActionBar(@NonNull Component message);

    /**
     * Sends an action bar message with explicit animation timing in ticks.
     *
     * @param message the action bar message
     * @param fadeIn  fade-in duration in ticks
     * @param stay    stay duration in ticks
     * @param fadeOut fade-out duration in ticks
     */
    void sendActionBar(Component message, int fadeIn, int stay, int fadeOut);

    /**
     * Sends a title with default animation timing.
     *
     * @param title the title text
     */
    void sendTitle(Component title);

    /**
     * Sends a title and subtitle with default animation timing.
     *
     * @param title    the title text
     * @param subtitle the subtitle text
     */
    void sendTitle(Component title, Component subtitle);

    /**
     * Sends a title and subtitle with explicit animation timing in ticks.
     *
     * @param title    the title text
     * @param subtitle the subtitle text
     * @param fadeIn   fade-in duration in ticks
     * @param stay     stay duration in ticks
     * @param fadeOut  fade-out duration in ticks
     */
    void sendTitle(Component title, Component subtitle, int fadeIn, int stay, int fadeOut);

    /**
     * Sends a subtitle without replacing the current title.
     *
     * @param subtitle the subtitle text
     */
    void sendSubtitle(Component subtitle);

    /**
     * Sets title animation timing in ticks.
     *
     * @param fadeIn  fade-in duration in ticks
     * @param stay    stay duration in ticks
     * @param fadeOut fade-out duration in ticks
     */
    void setTitleTimes(int fadeIn, int stay, int fadeOut);

    /**
     * Clears the currently displayed title.
     */
    @Override
    void clearTitle();

    /**
     * Resets title text and animation timings to the client defaults.
     */
    @Override
    void resetTitle();

    /**
     * Saves the player's current state, waiting for the write to complete.
     */
    default void save() {
        save(false);
    }

    /**
     * Saves the player's current state when player-data saving is enabled.
     *
     * @param async whether to return before the storage write completes
     * @throws IllegalStateException if the player is closed
     */
    void save(boolean async);

    /**
     * Returns the cardinal direction the player is facing.
     */
    CardinalDirection getCardinalDirection();

    /**
     * Returns the player's respawn location, falling back to the primary level's safe spawn
     * when no valid personal spawn is set.
     */
    Location getSpawn();

    /**
     * Sets the player's personal spawn point.
     * Fires {@link PlayerSetSpawnEvent}, which may cancel the change.
     * Use {@link #clearSpawn()} to clear the personal spawn.
     *
     * @param spawn the new spawn location
     */
    void setSpawn(Location spawn);

    /**
     * Sets the player's personal spawn point with an explicit cause.
     * Fires {@link PlayerSetSpawnEvent}, which may cancel the change.
     * Use {@link #clearSpawn()} to clear the personal spawn.
     *
     * @param spawn the new spawn location
     * @param cause the reason for the change (used as the event cause)
     */
    void setSpawn(Location spawn, PlayerSetSpawnEvent.Cause cause);

    /**
     * Clears the player's personal spawn point so they will respawn at the primary level's spawn.
     * Fires {@link PlayerSetSpawnEvent}, which may cancel the change.
     */
    void clearSpawn();

    /**
     * Returns the player's current skin.
     */
    Skin getSkin();

    /**
     * Changes the player's skin and updates its appearance for viewers.
     *
     * @param newSkin the replacement skin
     */
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
     * Returns the explicitly open inventory screen, including the player's own inventory,
     * or {@code null} when only the HUD is visible.
     * Use {@link #getCurrentScreen()} to include the HUD.
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
     * @return the {@link PlayerInventoryScreen} if the player's own inventory is open, or {@code null}
     */
    @Nullable
    PlayerInventoryScreen getInventoryScreen();

    /**
     * Returns the currently open inventory screen if it matches the given type, or {@code null} otherwise.
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
     * This method is server-initiated and will notify the player's game to close the UI.
     */
    void closeInventory();

    /**
     * Opens the container associated with the given block for this player.
     * The block must be a container block (e.g. a chest, furnace, or hopper).
     * Does nothing when the player already has an inventory screen open.
     *
     * @param block the container block to open
     * @throws IllegalArgumentException if the block is not a container
     */
    void openContainer(Block block);

    /**
     * Opens the container associated with the given block entity for this player.
     * The block entity must be a container (e.g. a chest, furnace, or hopper block entity).
     * Does nothing when the player already has an inventory screen open.
     *
     * @param blockEntity the container block entity to open
     * @throws IllegalArgumentException if the block entity is not a container
     */
    void openContainer(BlockEntity blockEntity);

    /**
     * Creates a new virtual single-chest (27-slot) view for this player.
     *
     * <p>The screen has no backing block in the level and is not opened automatically.
     * Call {@link #openInventory(InventoryScreen)} to show it.</p>
     *
     * @param title the title displayed in the GUI title bar
     * @return a new {@link VirtualChestScreen} bound to this player
     */
    VirtualChestScreen createVirtualChest(Component title);

    /**
     * Creates a new virtual double-chest (54-slot) screen for this player.
     *
     * <p>The screen has no backing block in the level and is not opened automatically.
     * Call {@link #openInventory(InventoryScreen)} to show it.</p>
     *
     * @param title the title displayed in the GUI title bar
     * @return a new {@link VirtualDoubleChestScreen} bound to this player
     */
    VirtualDoubleChestScreen createVirtualDoubleChest(Component title);

    /**
     * Creates a new virtual hopper (5-slot) screen for this player.
     *
     * <p>The screen has no backing block in the level and is not opened automatically.
     * Call {@link #openInventory(InventoryScreen)} to show it.</p>
     *
     * @param title the title displayed in the GUI title bar
     * @return a new {@link VirtualHopperScreen} bound to this player
     */
    VirtualHopperScreen createVirtualHopper(Component title);
}
