package org.cloudburstmc.server.command.defaults;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.kyori.adventure.text.Component;
import org.cloudburstmc.api.command.CommandSender;
import org.cloudburstmc.api.command.CommandSourceStack;
import org.cloudburstmc.api.command.Commands;
import org.cloudburstmc.api.command.argument.CommandArguments;
import org.cloudburstmc.api.command.argument.CommandArgumentTypes;
import org.cloudburstmc.server.CloudServer;
import org.cloudburstmc.server.command.AdvertisedCommand;
import org.cloudburstmc.server.command.CommandUtils;
import org.cloudburstmc.server.command.network.CommandNetworkData;

import java.util.List;
import java.util.StringJoiner;

public class WhitelistCommand extends AdvertisedCommand {

    public WhitelistCommand() {
        super("allowlist", "commands.whitelist.description", List.of("whitelist"), CommandNetworkData.OWNER,
                "cloudburst.command.whitelist.reload",
                "cloudburst.command.whitelist.enable", "cloudburst.command.whitelist.disable",
                "cloudburst.command.whitelist.list", "cloudburst.command.whitelist.add",
                "cloudburst.command.whitelist.remove");
    }

    @Override
    public void configure(LiteralArgumentBuilder<CommandSourceStack> builder, String label, CommandArguments arguments) {
        builder.then(Commands.literal("on")
                .requires(Commands.requiresPermission("cloudburst.command.whitelist.enable"))
                .executes(this::executeCommand));
        builder.then(Commands.literal("off")
                .requires(Commands.requiresPermission("cloudburst.command.whitelist.disable"))
                .executes(this::executeCommand));
        builder.then(Commands.literal("list")
                .requires(Commands.requiresPermission("cloudburst.command.whitelist.list"))
                .executes(this::executeCommand));
        builder.then(Commands.literal("reload")
                .requires(Commands.requiresPermission("cloudburst.command.whitelist.reload"))
                .executes(this::executeCommand));
        builder.then(Commands.literal("add")
                .requires(Commands.requiresPermission("cloudburst.command.whitelist.add"))
                .then(Commands.argument("player", CommandArgumentTypes.string())
                        .executes(this::executeCommand)));
        builder.then(Commands.literal("remove")
                .requires(Commands.requiresPermission("cloudburst.command.whitelist.remove"))
                .then(Commands.argument("player", CommandArgumentTypes.string())
                        .executes(this::executeCommand)));
    }

    @Override
    protected int execute(CommandContext<CommandSourceStack> context) {
        CommandSender sender = sender(context);
        CloudServer server = (CloudServer) sender.getServer();
        String action = action(context);

        if (!hasArgument(context, "player")) {
            switch (action) {
                case "reload":
                    server.reloadWhitelist();
                    CommandUtils.broadcastCommandMessage(sender, Component.translatable("commands.whitelist.reloaded"));
                    return success();
                case "on":
                    server.getConfig().setWhitelist(true);
                    CommandUtils.broadcastCommandMessage(sender, Component.translatable("commands.whitelist.enabled"));
                    return success();
                case "off":
                    server.getConfig().setWhitelist(false);
                    CommandUtils.broadcastCommandMessage(sender, Component.translatable("commands.whitelist.disabled"));
                    return success();
                case "list":
                    StringJoiner result = new StringJoiner(", ");
                    int count = 0;
                    for (String player : server.getWhitelist().getAll().keySet()) {
                        result.add(player);
                        ++count;
                    }
                    sender.sendMessage(Component.translatable("commands.whitelist.list", Component.text(count), Component.text(count)));
                    sender.sendMessage(Component.text(result.toString()));
                    return success();
            }
        } else {
            String player = argumentValue(context, "player");
            switch (action) {
                case "add":
                    server.getOfflinePlayer(player).setWhitelisted(true);
                    CommandUtils.broadcastCommandMessage(sender, Component.translatable("commands.whitelist.add.success", Component.text(player)));
                    return success();
                case "remove":
                    server.getOfflinePlayer(player).setWhitelisted(false);
                    CommandUtils.broadcastCommandMessage(sender, Component.translatable("commands.whitelist.remove.success", Component.text(player)));
                    return success();
            }
        }

        return success();
    }

    private static String action(CommandContext<CommandSourceStack> context) {
        for (String action : new String[]{"on", "off", "list", "reload", "add", "remove"}) {
            if (hasArgument(context, action)) {
                return action;
            }
        }

        throw new IllegalStateException("Whitelist command has no action");
    }
}
