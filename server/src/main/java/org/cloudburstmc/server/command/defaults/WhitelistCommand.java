package org.cloudburstmc.server.command.defaults;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.kyori.adventure.text.Component;
import org.cloudburstmc.api.command.CommandSender;
import org.cloudburstmc.api.command.CommandSourceStack;
import org.cloudburstmc.api.command.Commands;
import org.cloudburstmc.api.command.argument.CommandArgumentTypes;
import org.cloudburstmc.api.command.argument.CommandArguments;
import org.cloudburstmc.api.player.OfflinePlayer;
import org.cloudburstmc.server.CloudServer;
import org.cloudburstmc.server.command.AdvertisedCommand;
import org.cloudburstmc.server.command.CommandUtils;
import org.cloudburstmc.server.command.network.CommandNetworkData;

import java.util.List;
import java.util.Set;
import java.util.StringJoiner;

public class WhitelistCommand extends AdvertisedCommand {

    public WhitelistCommand() {
        super("whitelist", "cloudburst.command.whitelist.description", List.of(), CommandNetworkData.DEFAULT,
                "cloudburst.command.whitelist.reload",
                "cloudburst.command.whitelist.enable", "cloudburst.command.whitelist.disable",
                "cloudburst.command.whitelist.list", "cloudburst.command.whitelist.add",
                "cloudburst.command.whitelist.remove");
    }

    @Override
    public void configure(LiteralArgumentBuilder<CommandSourceStack> builder, String label, CommandArguments arguments) {
        builder.then(Commands.literal("on")
                .requires(Commands.requiresPermission("cloudburst.command.whitelist.enable"))
                .executes(context -> this.executeCommand(context, this::enable)));
        builder.then(Commands.literal("off")
                .requires(Commands.requiresPermission("cloudburst.command.whitelist.disable"))
                .executes(context -> this.executeCommand(context, this::disable)));
        builder.then(Commands.literal("list")
                .requires(Commands.requiresPermission("cloudburst.command.whitelist.list"))
                .executes(context -> this.executeCommand(context, this::list)));
        builder.then(Commands.literal("reload")
                .requires(Commands.requiresPermission("cloudburst.command.whitelist.reload"))
                .executes(context -> this.executeCommand(context, this::reload)));
        builder.then(Commands.literal("add")
                .requires(Commands.requiresPermission("cloudburst.command.whitelist.add"))
                .then(Commands.argument("player", CommandArgumentTypes.string())
                        .executes(context -> this.executeCommand(context, this::add))));
        builder.then(Commands.literal("remove")
                .requires(Commands.requiresPermission("cloudburst.command.whitelist.remove"))
                .then(Commands.argument("player", CommandArgumentTypes.string())
                        .executes(context -> this.executeCommand(context, this::remove))));
    }

    @Override
    protected int execute(CommandContext<CommandSourceStack> context) {
        return usage();
    }

    private int enable(CommandContext<CommandSourceStack> context) {
        if (server(context).hasWhitelist()) {
            return failure(context, Component.translatable("cloudburst.command.whitelist.alreadyEnabled"));
        }

        server(context).setWhitelist(true);
        CommandUtils.broadcastCommandMessage(sender(context), Component.translatable("cloudburst.command.whitelist.enabled"));
        return success();
    }

    private int disable(CommandContext<CommandSourceStack> context) {
        if (!server(context).hasWhitelist()) {
            return failure(context, Component.translatable("cloudburst.command.whitelist.alreadyDisabled"));
        }

        server(context).setWhitelist(false);
        CommandUtils.broadcastCommandMessage(sender(context), Component.translatable("cloudburst.command.whitelist.disabled"));
        return success();
    }

    private int list(CommandContext<CommandSourceStack> context) {
        Set<OfflinePlayer> players = server(context).getWhitelistedPlayers();
        if (players.isEmpty()) {
            sender(context).sendMessage(Component.translatable("cloudburst.command.whitelist.empty"));
            return success();
        }

        StringJoiner names = new StringJoiner(", ");
        for (OfflinePlayer player : players) {
            names.add(player.getName() == null ? player.getUniqueId().toString() : player.getName());
        }

        sender(context).sendMessage(Component.translatable("cloudburst.command.whitelist.players",
                Component.text(players.size()), Component.text(names.toString())));
        return success();
    }

    private int reload(CommandContext<CommandSourceStack> context) {
        server(context).reloadWhitelist();
        CommandUtils.broadcastCommandMessage(sender(context), Component.translatable("cloudburst.command.whitelist.reloaded"));
        return success();
    }

    private int add(CommandContext<CommandSourceStack> context) {
        CommandSender sender = sender(context);
        String player = argumentValue(context, "player");
        OfflinePlayer target = server(context).getOfflinePlayer(player);
        if (target.isWhitelisted()) {
            return failure(context, Component.translatable("cloudburst.command.whitelist.alreadyAdded", Component.text(player)));
        }

        target.setWhitelisted(true);
        CommandUtils.broadcastCommandMessage(sender, Component.translatable("cloudburst.command.whitelist.added", Component.text(player)));
        return success();
    }

    private int remove(CommandContext<CommandSourceStack> context) {
        CommandSender sender = sender(context);
        String player = argumentValue(context, "player");
        OfflinePlayer target = server(context).getOfflinePlayer(player);
        if (!target.isWhitelisted()) {
            return failure(context, Component.translatable("cloudburst.command.whitelist.notAdded", Component.text(player)));
        }

        target.setWhitelisted(false);
        CommandUtils.broadcastCommandMessage(sender, Component.translatable("cloudburst.command.whitelist.removed", Component.text(player)));
        return success();
    }

    private CloudServer server(CommandContext<CommandSourceStack> context) {
        return (CloudServer) sender(context).getServer();
    }
}
