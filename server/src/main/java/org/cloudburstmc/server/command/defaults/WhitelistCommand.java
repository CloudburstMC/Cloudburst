package org.cloudburstmc.server.command.defaults;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.kyori.adventure.text.Component;
import org.cloudburstmc.api.command.CommandSender;
import org.cloudburstmc.api.command.CommandSourceStack;
import org.cloudburstmc.api.command.Commands;
import org.cloudburstmc.api.command.argument.CommandArgumentTypes;
import org.cloudburstmc.api.command.argument.CommandArguments;
import org.cloudburstmc.server.CloudServer;
import org.cloudburstmc.server.command.AdvertisedCommand;
import org.cloudburstmc.server.command.CommandUtils;
import org.cloudburstmc.server.command.network.CommandNetworkData;

import java.util.List;
import java.util.StringJoiner;

public class WhitelistCommand extends AdvertisedCommand {

    public WhitelistCommand() {
        super("allowlist", "commands.whitelist.description", List.of("whitelist"), CommandNetworkData.DEFAULT,
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
        CommandSender sender = sender(context);
        server(context).getConfig().setWhitelist(true);
        CommandUtils.broadcastCommandMessage(sender, Component.translatable("commands.whitelist.enabled"));
        return success();
    }

    private int disable(CommandContext<CommandSourceStack> context) {
        CommandSender sender = sender(context);
        server(context).getConfig().setWhitelist(false);
        CommandUtils.broadcastCommandMessage(sender, Component.translatable("commands.whitelist.disabled"));
        return success();
    }

    private int list(CommandContext<CommandSourceStack> context) {
        CommandSender sender = sender(context);
        StringJoiner result = new StringJoiner(", ");
        int count = 0;
        for (String player : server(context).getWhitelist().getAll().keySet()) {
            result.add(player);
            count++;
        }
        sender.sendMessage(Component.translatable("commands.whitelist.list", Component.text(count), Component.text(count)));
        sender.sendMessage(Component.text(result.toString()));
        return success();
    }

    private int reload(CommandContext<CommandSourceStack> context) {
        CommandSender sender = sender(context);
        server(context).reloadWhitelist();
        CommandUtils.broadcastCommandMessage(sender, Component.translatable("commands.whitelist.reloaded"));
        return success();
    }

    private int add(CommandContext<CommandSourceStack> context) {
        CommandSender sender = sender(context);
        String player = argumentValue(context, "player");
        server(context).getOfflinePlayer(player).setWhitelisted(true);
        CommandUtils.broadcastCommandMessage(sender, Component.translatable("commands.whitelist.add.success", Component.text(player)));
        return success();
    }

    private int remove(CommandContext<CommandSourceStack> context) {
        CommandSender sender = sender(context);
        String player = argumentValue(context, "player");
        server(context).getOfflinePlayer(player).setWhitelisted(false);
        CommandUtils.broadcastCommandMessage(sender, Component.translatable("commands.whitelist.remove.success", Component.text(player)));
        return success();
    }

    private CloudServer server(CommandContext<CommandSourceStack> context) {
        return (CloudServer) sender(context).getServer();
    }
}
