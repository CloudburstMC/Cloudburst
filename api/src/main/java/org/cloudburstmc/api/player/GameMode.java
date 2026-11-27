package org.cloudburstmc.api.player;

import net.kyori.adventure.translation.Translatable;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;

/**
 * A player game mode, defining what actions a player may perform
 * and which {@link Ability abilities} they hold by default.
 */
public enum GameMode implements Translatable {
    /**
     * Normal gameplay: the player can build, mine, interact with the world,
     * and fight mobs, but has no special privileges.
     */
    SURVIVAL(0, EnumSet.of(
            Ability.BUILD,
            Ability.MINE,
            Ability.DOORS_AND_SWITCHES,
            Ability.OPEN_CONTAINERS,
            Ability.ATTACK_PLAYERS,
            Ability.ATTACK_MOBS)
    ),
    /**
     * Creative mode: the player is invulnerable, can fly, breaks blocks
     * instantly, and has access to all items.
     */
    CREATIVE(1, EnumSet.of(
            Ability.BUILD,
            Ability.MINE,
            Ability.DOORS_AND_SWITCHES,
            Ability.OPEN_CONTAINERS,
            Ability.ATTACK_PLAYERS,
            Ability.ATTACK_MOBS,
            Ability.INVULNERABLE,
            Ability.MAY_FLY,
            Ability.INSTABUILD)
    ),
    /**
     * Adventure mode: the player can interact with the world and fight mobs,
     * but can only place and break blocks that their held item allows.
     */
    ADVENTURE(2, EnumSet.of(
            Ability.BUILD,
            Ability.MINE,
            Ability.DOORS_AND_SWITCHES,
            Ability.OPEN_CONTAINERS,
            Ability.ATTACK_PLAYERS,
            Ability.ATTACK_MOBS)
    ),
    /**
     * Spectator mode: the player is invulnerable, always flying, passes through
     * blocks, and cannot interact with anything.
     */
    SPECTATOR(3, EnumSet.of(
            Ability.BUILD,
            Ability.MINE,
            Ability.DOORS_AND_SWITCHES,
            Ability.OPEN_CONTAINERS,
            Ability.ATTACK_PLAYERS,
            Ability.ATTACK_MOBS,
            Ability.INVULNERABLE,
            Ability.MAY_FLY,
            Ability.INSTABUILD)
    );

    private final int value;
    private final Set<Ability> defaultAbilities;

    GameMode(int value, Set<Ability> defaultAbilities) {
        this.value = value;
        this.defaultAbilities = Collections.unmodifiableSet(defaultAbilities);
    }

    /**
     * Returns the numeric ID of this game mode, from 0 to 3.
     */
    public int getValue() {
        return this.value;
    }

    /**
     * Returns the game mode with the given numeric ID.
     *
     * @param value the numeric ID
     * @return the game mode, or {@code null} when the ID is unknown
     */
    public static @Nullable GameMode getByValue(int value) {
        for (GameMode mode : values()) {
            if (mode.value == value) {
                return mode;
            }
        }

        return null;
    }

    /**
     * Returns the {@link Ability abilities} granted to a player in this game mode.
     * The server may additionally grant operator abilities such as
     * {@link Ability#OPERATOR_COMMANDS} and {@link Ability#TELEPORT}.
     */
    public Set<Ability> getDefaultAbilities() {
        return this.defaultAbilities;
    }

    /**
     * Returns whether this game mode grants {@link Ability#INVULNERABLE}.
     */
    public boolean isInvulnerable() {
        return this.defaultAbilities.contains(Ability.INVULNERABLE);
    }

    @Override
    public @NonNull String translationKey() {
        return "gameMode." + this.name().toLowerCase(Locale.ROOT);
    }
}
