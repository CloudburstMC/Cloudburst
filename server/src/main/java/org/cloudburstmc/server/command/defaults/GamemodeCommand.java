package org.cloudburstmc.server.command.defaults;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.kyori.adventure.text.Component;
import org.cloudburstmc.api.command.CommandSender;
import org.cloudburstmc.api.command.CommandSourceStack;
import org.cloudburstmc.api.command.Commands;
import org.cloudburstmc.api.command.argument.CommandArguments;
import org.cloudburstmc.api.command.argument.CommandArgumentTypes;
import org.cloudburstmc.api.event.player.PlayerGameModeChangeEvent;
import org.cloudburstmc.api.player.GameMode;
import org.cloudburstmc.server.CloudServer;
import org.cloudburstmc.server.command.CommandUtils;
import org.cloudburstmc.server.command.AdvertisedCommand;
import org.cloudburstmc.server.command.network.CommandNetworkData;
import org.cloudburstmc.server.player.CloudPlayer;

import java.util.List;
import java.util.Locale;

public class GamemodeCommand extends AdvertisedCommand {

    public GamemodeCommand() {
        super("gamemode", "commands.gamemode.description", CommandNetworkData.GAME_DIRECTORS,
                "cloudburst.command.gamemode");
    }

    @Override
    public void configure(LiteralArgumentBuilder<CommandSourceStack> builder, String label, CommandArguments arguments) {
        var namedMode = Commands.argument("gameMode", CommandArgumentTypes.fixedEnumNamed("gameMode", "GameMode",
                "default", "creative", "spectator", "survival", "adventure", "d", "c", "s", "a"))
                .executes(this::executeCommand);
        namedMode.then(Commands.argument("player", arguments.players())
                .requires(Commands.requiresPermission("cloudburst.command.gamemode.other"))
                .executes(this::executeCommand));
        builder.then(namedMode);

        var numericMode = Commands.argument("gameModeValue", CommandArgumentTypes.integer("gameMode", 0, 3))
                .executes(this::executeCommand);
        numericMode.then(Commands.argument("player", arguments.players())
                .requires(Commands.requiresPermission("cloudburst.command.gamemode.other"))
                .executes(this::executeCommand));
        builder.then(numericMode);
    }

    @Override
    protected int execute(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSender sender = sender(context);
        String gameModeValue = hasArgument(context, "gameMode")
                ? argumentValue(context, "gameMode")
                : String.valueOf(argumentValue(context, "gameModeValue", Integer.class));
        GameMode gameMode = parseGameMode(gameModeValue);
        if (gameMode == null) {
            return failure(context, Component.text("Unknown game mode"));
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
                sender.sendMessage(Component.text("Game mode update for " + target.getName() + " failed"));
                continue;
            }

            boolean changed = target.setGamemode(gameMode, PlayerGameModeChangeEvent.Cause.COMMAND);
            if (!changed) {
                sender.sendMessage(Component.text("Game mode update for " + target.getName() + " failed"));
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

    private static GameMode parseGameMode(String input) {
        String value = input.toLowerCase(Locale.ROOT);
        if ("default".equals(value) || "d".equals(value)) {
            return CloudServer.getInstance().getGameMode();
        }
        return GameMode.from(value);
    }
}
