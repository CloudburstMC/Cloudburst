package org.cloudburstmc.api.player;

import com.google.common.collect.Sets;
import lombok.Getter;
import net.kyori.adventure.translation.Translatable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Represents a player game mode, defining what actions a player may perform
 * and which {@link Ability abilities} they hold by default.
 */
@Getter
public class GameMode implements Translatable {

    private static final Map<String, GameMode> nameMap = new HashMap<>();

    /**
     * Normal gameplay: the player can build, mine, interact with the world,
     * and fight mobs, but has no special privileges.
     */
    public static final GameMode SURVIVAL = GameMode.builder(0, "survival", "s")
            .register()
            .abilities(
                    Ability.BUILD,
                    Ability.MINE,
                    Ability.DOORS_AND_SWITCHES,
                    Ability.OPEN_CONTAINERS,
                    Ability.ATTACK_PLAYERS,
                    Ability.ATTACK_MOBS
            ).build();

    /**
     * Creative mode: the player is invulnerable, can fly, breaks blocks
     * instantly, and has access to all items.
     */
    public static final GameMode CREATIVE = GameMode.builder(1, "creative", "c")
            .register()
            .abilities(
                    Ability.BUILD,
                    Ability.MINE,
                    Ability.DOORS_AND_SWITCHES,
                    Ability.OPEN_CONTAINERS,
                    Ability.ATTACK_PLAYERS,
                    Ability.ATTACK_MOBS,
                    Ability.INVULNERABLE,
                    Ability.MAY_FLY,
                    Ability.INSTABUILD
            ).build();

    /**
     * Adventure mode: the player can interact with the world and fight mobs.
     * Block placement and breaking are restricted server-side by item tags;
     * {@link Ability#BUILD} and {@link Ability#MINE} remain enabled in the
     * abilities layer so the game behaves correctly.
     */
    public static final GameMode ADVENTURE = GameMode.builder(2, "adventure", "a")
            .register()
            .abilities(
                    Ability.BUILD,
                    Ability.MINE,
                    Ability.DOORS_AND_SWITCHES,
                    Ability.OPEN_CONTAINERS,
                    Ability.ATTACK_PLAYERS,
                    Ability.ATTACK_MOBS
            ).build();

    /**
     * Spectator mode: the player is invulnerable, always flying, passes through
     * blocks, and cannot interact with anything.
     */
    public static final GameMode SPECTATOR = GameMode.builder(4, "spectator", "spc")
            .register()
            .abilities(
                    Ability.BUILD,
                    Ability.MINE,
                    Ability.DOORS_AND_SWITCHES,
                    Ability.OPEN_CONTAINERS,
                    Ability.ATTACK_PLAYERS,
                    Ability.ATTACK_MOBS,
                    Ability.INVULNERABLE,
                    Ability.MAY_FLY,
                    Ability.INSTABUILD
            ).build();

    private final int vanillaId;
    private final String name;
    private final Set<String> aliases;

    /**
     * The set of {@link Ability abilities} granted to a player when they enter
     * this game mode. This is the baseline; the server may additionally grant
     * operator-only abilities such as {@link Ability#OPERATOR_COMMANDS} and
     * {@link Ability#TELEPORT} depending on the player's permissions.
     */
    private final Set<Ability> defaultAbilities;

    private GameMode(int vanillaId, String name, Set<String> aliases, Set<Ability> defaultAbilities) {
        this.vanillaId = vanillaId;
        this.name = name;
        this.aliases = aliases;
        this.defaultAbilities = Collections.unmodifiableSet(defaultAbilities);
    }

    /**
     * Looks up a registered game mode by name or alias (case-sensitive).
     * Returns {@code null} if no game mode is registered under the given name.
     *
     * @param name the name or alias to look up
     * @return the matching {@link GameMode}, or {@code null}
     */
    @Nullable
    public static GameMode from(String name) {
        return nameMap.get(name);
    }

    /**
     * Resolves a {@link GameMode} from a numeric game type ID.
     *
     * @param id the numeric game type value
     * @return the corresponding {@link GameMode}
     */
    @NotNull
    public static GameMode from(int id) {
        return switch (id & 0x03) {
            case 0 -> SURVIVAL;
            case 1 -> CREATIVE;
            case 2 -> ADVENTURE;
            default -> SPECTATOR;
        };
    }

    /**
     * Creates a new {@link Builder} for a custom game mode.
     *
     * @param id      the numeric game type ID
     * @param name    the primary name, used for commands and lookup
     * @param aliases additional lookup strings (the numeric ID is always added automatically)
     * @return a new {@link Builder}
     */
    @NotNull
    public static Builder builder(int id, String name, String... aliases) {
        return new Builder(id, name, aliases);
    }

    /**
     * Returns the translation key for this game mode, e.g. {@code "gameMode.survival"}.
     */
    @Override
    public @NotNull String translationKey() {
        return "gameMode." + this.name;
    }

    /**
     * Returns whether this game mode grants {@link Ability#INVULNERABLE}.
     * This is true for {@link #CREATIVE} and {@link #SPECTATOR}.
     */
    public boolean isInvulnerable() {
        return defaultAbilities.contains(Ability.INVULNERABLE);
    }

    /**
     * Builder for constructing custom {@link GameMode} instances.
     */
    public static class Builder {

        private final int vanillaId;
        private final String name;
        private final Set<String> aliases;
        private final Set<Ability> defaultAbilities = EnumSet.noneOf(Ability.class);

        private boolean register = false;

        private Builder(int vanillaId, String name, String... aliases) {
            this.vanillaId = vanillaId;
            this.name = name;
            this.aliases = Sets.newHashSet(aliases);
            this.aliases.add(Integer.toString(vanillaId));
        }

        /**
         * Registers this game mode in the global name map so it can be
         * resolved by {@link GameMode#from(String)} and
         * {@link GameMode#from(int)}.
         *
         * @return this builder
         */
        public Builder register() {
            register = true;
            return this;
        }

        /**
         * Declares the {@link Ability abilities} that players receive by default
         * when assigned this game mode. Calling this method multiple times
         * accumulates abilities.
         *
         * @param abilities the abilities to grant
         * @return this builder
         */
        public Builder abilities(Ability... abilities) {
            Collections.addAll(this.defaultAbilities, abilities);
            return this;
        }

        /**
         * Builds and optionally registers the {@link GameMode}.
         *
         * @return the constructed {@link GameMode}
         */
        public GameMode build() {
            GameMode gm = new GameMode(vanillaId, name, aliases, defaultAbilities);

            if (register) {
                nameMap.put(name, gm);
                for (String alias : gm.aliases) {
                    nameMap.put(alias, gm);
                }
            }

            return gm;
        }
    }
}
