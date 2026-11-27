package org.cloudburstmc.server.command.defaults;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.kyori.adventure.text.Component;
import org.cloudburstmc.api.command.CommandSender;
import org.cloudburstmc.api.command.CommandSourceStack;
import org.cloudburstmc.api.command.Commands;
import org.cloudburstmc.api.command.argument.CommandArgumentTypes;
import org.cloudburstmc.api.command.argument.CommandArguments;
import org.cloudburstmc.api.event.player.PlayerGameModeChangeCause;
import org.cloudburstmc.api.player.GameMode;
import org.cloudburstmc.protocol.bedrock.data.command.CommandPermission;
import org.cloudburstmc.server.command.AdvertisedCommand;
import org.cloudburstmc.server.command.CommandUtils;
import org.cloudburstmc.server.command.argument.GameModeArguments;
import org.cloudburstmc.server.command.network.CommandNetworkData;
import org.cloudburstmc.server.player.CloudPlayer;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class GamemodeCommand extends AdvertisedCommand {

    static final CommandNetworkData NETWORK_DATA = new CommandNetworkData(Set.of(), CommandPermission.GAME_DIRECTORS, Map.of());

    public GamemodeCommand() {
        super("gamemode", "commands.gamemode.description", NETWORK_DATA, "cloudburst.command.gamemode");
    }

    @Override
    public void configure(LiteralArgumentBuilder<CommandSourceStack> builder, String label, CommandArguments arguments) {
        RequiredArgumentBuilder<CommandSourceStack, String> namedMode =
                Commands.argument("gameMode", CommandArgumentTypes.fixedEnumNamed("gameMode", "GameMode",
                                "default", "creative", "spectator", "survival", "adventure", "d", "c", "s", "a"))
                        .executes(this::executeCommand);
        namedMode.then(Commands.argument("player", arguments.players())
                .requires(Commands.requiresPermission("cloudburst.command.gamemode.other"))
                .executes(this::executeCommand));
        builder.then(namedMode);

        RequiredArgumentBuilder<CommandSourceStack, Integer> numericMode =
                Commands.argument("gameModeValue", CommandArgumentTypes.integer("gameMode"))
                        .executes(this::executeCommand);
        numericMode.then(Commands.argument("player", arguments.players())
                .requires(Commands.requiresPermission("cloudburst.command.gamemode.other"))
                .executes(this::executeCommand));
        builder.then(numericMode);
    }

    @Override
    protected int execute(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSender sender = sender(context);
        String input = hasArgument(context, "gameMode")
                ? argumentValue(context, "gameMode")
                : String.valueOf(argumentValue(context, "gameModeValue", Integer.class));
        GameMode gameMode = switch (input) {
            case "default", "d", "5" -> sender.getServer().getDefaultGameMode();
            default -> GameModeArguments.gameMode(input);
        };
        if (gameMode == null) {
            return failure(context, Component.translatable("commands.gamemode.fail.invalid", Component.text(input)));
        }

        List<CloudPlayer> targets;
        if (hasArgument(context, "player")) {
            targets = cloudPlayersArgument(context, "player");
            if (targets.isEmpty()) {
                return success();
            }
        } else {
            if (!(sender instanceof CloudPlayer player)) {
                return usage();
            }
            targets = List.of(player);
        }

        for (CloudPlayer target : targets) {
            if (target.getGameMode() == gameMode) {
                sender.sendMessage(Component.translatable("cloudburst.command.gamemode.failed", Component.text(target.getName())));
                continue;
            }

            boolean changed = target.setGameMode(gameMode, PlayerGameModeChangeCause.COMMAND);
            if (!changed) {
                sender.sendMessage(Component.translatable("cloudburst.command.gamemode.failed", Component.text(target.getName())));
            } else if (target.equals(sender)) {
                CommandUtils.broadcastCommandMessage(sender, Component.translatable("commands.gamemode.success.self", Component.translatable(gameMode)));
            } else {
                target.sendMessage(Component.translatable("gameMode.changed"));
                CommandUtils.broadcastCommandMessage(sender, Component.translatable("commands.gamemode.success.other",
                        Component.text(target.getName()), Component.translatable(gameMode)));
            }
        }

        return success();
    }
}
