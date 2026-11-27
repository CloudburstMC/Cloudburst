package org.cloudburstmc.server.command.argument;

import lombok.experimental.UtilityClass;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.player.GameMode;

/**
 * Game mode values accepted in commands and selectors: names, short names and numbers as the client lists them.
 */
@UtilityClass
public class GameModeArguments {

    public static @Nullable GameMode gameMode(String input) {
        return switch (input) {
            case "survival", "s", "0" -> GameMode.SURVIVAL;
            case "creative", "c", "1" -> GameMode.CREATIVE;
            case "adventure", "a", "2" -> GameMode.ADVENTURE;
            case "spectator", "6" -> GameMode.SPECTATOR;
            default -> null;
        };
    }
}
