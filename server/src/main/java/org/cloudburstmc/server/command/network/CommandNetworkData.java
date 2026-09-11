package org.cloudburstmc.server.command.network;

import org.cloudburstmc.protocol.bedrock.data.command.CommandData;
import org.cloudburstmc.protocol.bedrock.data.command.CommandPermission;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * Client presentation metadata for a command root.
 */
public record CommandNetworkData(Set<CommandData.Flag> flags, CommandPermission permission) {
    public static final CommandNetworkData ANY_NOT_CHEAT = of(CommandPermission.ANY, CommandData.Flag.NOT_CHEAT);
    public static final CommandNetworkData ANY_MESSAGE_NOT_CHEAT = of(CommandPermission.ANY, CommandData.Flag.MESSAGE_TYPE, CommandData.Flag.NOT_CHEAT);
    public static final CommandNetworkData GAME_DIRECTORS = of(CommandPermission.GAME_DIRECTORS);
    public static final CommandNetworkData GAME_DIRECTORS_NOT_CHEAT = of(CommandPermission.GAME_DIRECTORS, CommandData.Flag.NOT_CHEAT);
    public static final CommandNetworkData GAME_DIRECTORS_MESSAGE = of(CommandPermission.GAME_DIRECTORS, CommandData.Flag.MESSAGE_TYPE);
    public static final CommandNetworkData GAME_DIRECTORS_MESSAGE_NOT_CHEAT = of(CommandPermission.GAME_DIRECTORS, CommandData.Flag.MESSAGE_TYPE, CommandData.Flag.NOT_CHEAT);
    public static final CommandNetworkData ADMIN_NOT_CHEAT = of(CommandPermission.ADMIN, CommandData.Flag.NOT_CHEAT);
    public static final CommandNetworkData OWNER = of(CommandPermission.OWNER);

    public CommandNetworkData {
        Objects.requireNonNull(flags, "flags");
        EnumSet<CommandData.Flag> orderedFlags = EnumSet.noneOf(CommandData.Flag.class);
        orderedFlags.addAll(flags);
        flags = Collections.unmodifiableSet(orderedFlags);
        Objects.requireNonNull(permission, "permission");
    }

    private static CommandNetworkData of(CommandPermission permission, CommandData.Flag... flags) {
        return new CommandNetworkData(Set.of(flags), permission);
    }
}
