package org.cloudburstmc.server.command.network;

import org.cloudburstmc.protocol.bedrock.data.command.CommandData;
import org.cloudburstmc.protocol.bedrock.data.command.CommandPermission;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Client presentation metadata for a command root.
 */
public record CommandNetworkData(Set<CommandData.Flag> flags, CommandPermission permission, Map<String, LiteralParameter> literalParameters) {
    public static final CommandNetworkData DEFAULT = of();
    public static final CommandNetworkData NOT_CHEAT = of(CommandData.Flag.NOT_CHEAT);
    public static final CommandNetworkData MESSAGE = of(CommandData.Flag.MESSAGE_TYPE);
    public static final CommandNetworkData MESSAGE_NOT_CHEAT = of(CommandData.Flag.MESSAGE_TYPE, CommandData.Flag.NOT_CHEAT);

    public CommandNetworkData {
        Objects.requireNonNull(flags, "flags");
        EnumSet<CommandData.Flag> orderedFlags = EnumSet.noneOf(CommandData.Flag.class);
        orderedFlags.addAll(flags);
        flags = Collections.unmodifiableSet(orderedFlags);
        Objects.requireNonNull(permission, "permission");
        literalParameters = Map.copyOf(literalParameters);
    }

    private static CommandNetworkData of(CommandData.Flag... flags) {
        return new CommandNetworkData(Set.of(flags), CommandPermission.ANY, Map.of());
    }

    public record LiteralParameter(String name, String enumName) {

        public LiteralParameter {
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(enumName, "enumName");
        }
    }
}
