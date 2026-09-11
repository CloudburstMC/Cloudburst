package org.cloudburstmc.server.command;

import co.aikar.timings.Timing;
import co.aikar.timings.Timings;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.kyori.adventure.text.Component;
import org.cloudburstmc.api.command.CommandSender;
import org.cloudburstmc.api.command.CommandSourceStack;
import org.cloudburstmc.api.command.argument.CommandArguments;
import org.cloudburstmc.api.command.argument.CommandArgumentTypes;
import org.cloudburstmc.api.permission.Permission;
import org.cloudburstmc.server.CloudServer;
import org.cloudburstmc.server.player.CloudPlayer;
import org.cloudburstmc.server.registry.CloudCommandRegistry;

import java.util.*;

/**
 * Common execution and metadata support for commands provided by the server.
 *
 * <p>Commands are dispatched server-side by default. Commands that should also be sent to clients extend
 * {@link AdvertisedCommand}.</p>
 */
public abstract class ServerCommand {
    private static final int COMMAND_FAILURE = 0;

    private final String name;
    private final String description;
    private final List<String> aliases;
    private final List<String> accessPermissions;
    private final Timing timing;

    protected ServerCommand(
            String name,
            String description,
            Collection<String> aliases,
            Collection<String> accessPermissions
    ) {
        this.name = Objects.requireNonNull(name, "name").toLowerCase(Locale.ROOT);
        this.description = Objects.requireNonNull(description, "description");
        this.aliases = List.copyOf(Objects.requireNonNull(aliases, "aliases"));
        this.accessPermissions = Objects.requireNonNull(accessPermissions, "accessPermissions").stream()
                .map(Permission::normalizeName)
                .distinct()
                .toList();
        this.timing = Timings.getCommandTiming(this);
    }

    protected ServerCommand(String name, String description, Collection<String> aliases, String... permissions) {
        this(name, description, aliases, Arrays.asList(permissions));
    }

    protected ServerCommand(String name, String description, String... permissions) {
        this(name, description, List.of(), Arrays.asList(permissions));
    }

    protected abstract int execute(CommandContext<CommandSourceStack> context) throws CommandSyntaxException;

    public void configure(LiteralArgumentBuilder<CommandSourceStack> builder, String label, CommandArguments arguments) {
        builder.executes(this::executeCommand);
    }

    protected final int executeCommand(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        this.timing.startTiming();
        try {
            return this.execute(context);
        } catch (CommandUsageException e) {
            String label = context.getNodes().getFirst().getNode().getName();
            CloudCommandRegistry registry = CloudServer.getInstance().getCommandRegistry();
            CommandUtils.sendUsage(context.getSource().sender(), registry.usages(label, context.getSource()));
            return com.mojang.brigadier.Command.SINGLE_SUCCESS;
        } finally {
            this.timing.stopTiming();
        }
    }

    protected static int success() {
        return com.mojang.brigadier.Command.SINGLE_SUCCESS;
    }

    protected static int success(CommandContext<CommandSourceStack> context, Component message) {
        context.getSource().sendSuccess(message);
        return success();
    }

    protected static int broadcastSuccess(CommandContext<CommandSourceStack> context, Component message) {
        context.getSource().broadcastSuccess(message);
        return success();
    }

    protected static int failure(CommandContext<CommandSourceStack> context, Component message) {
        context.getSource().sendFailure(message);
        return COMMAND_FAILURE;
    }

    protected static int usage() {
        throw new CommandUsageException();
    }

    protected static CommandSender sender(CommandContext<CommandSourceStack> context) {
        return context.getSource().sender();
    }

    protected static boolean hasArgument(CommandContext<CommandSourceStack> context, String name) {
        return context.getNodes().stream().anyMatch(parsedNode -> parsedNode.getNode().getName().equals(name));
    }

    protected static String argumentValue(CommandContext<CommandSourceStack> context, String name) {
        return context.getArgument(name, String.class);
    }

    protected static <T> T argumentValue(CommandContext<CommandSourceStack> context, String name, Class<T> type) {
        return context.getArgument(name, type);
    }

    protected static List<CloudPlayer> cloudPlayersArgument(CommandContext<CommandSourceStack> context, String name)
            throws CommandSyntaxException {
        return CommandArgumentTypes.players(context, name).stream()
                .map(CloudPlayer.class::cast)
                .toList();
    }

    public final String getName() {
        return this.name;
    }

    public final String getDescription() {
        return this.description;
    }

    public final Collection<String> getAliases() {
        return this.aliases;
    }

    public final Collection<String> getAccessPermissions() {
        return this.accessPermissions;
    }

    @Override
    public String toString() {
        return this.name;
    }
}
