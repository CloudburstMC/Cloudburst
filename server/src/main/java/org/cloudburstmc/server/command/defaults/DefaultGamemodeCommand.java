package org.cloudburstmc.server.command.defaults;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.kyori.adventure.text.Component;
import org.cloudburstmc.api.command.CommandSender;
import org.cloudburstmc.api.command.CommandSourceStack;
import org.cloudburstmc.api.command.Commands;
import org.cloudburstmc.api.command.argument.CommandArguments;
import org.cloudburstmc.api.command.argument.CommandArgumentTypes;
import org.cloudburstmc.api.player.GameMode;
import org.cloudburstmc.server.CloudServer;
import org.cloudburstmc.server.command.AdvertisedCommand;
import org.cloudburstmc.server.command.network.CommandNetworkData;

import java.util.Locale;

public class DefaultGamemodeCommand extends AdvertisedCommand {

    public DefaultGamemodeCommand() {
        super("defaultgamemode", "commands.defaultgamemode.description", CommandNetworkData.GAME_DIRECTORS,
                "cloudburst.command.defaultgamemode");
    }

    @Override
    public void configure(LiteralArgumentBuilder<CommandSourceStack> builder, String label, CommandArguments arguments) {
        builder.then(Commands.argument("gameMode", CommandArgumentTypes.fixedEnumNamed("gameMode", "DefaultGameMode",
                        "survival", "creative", "adventure", "spectator", "s", "c", "a", "view", "v"))
                .executes(this::executeCommand));
        builder.then(Commands.argument("gameModeValue", CommandArgumentTypes.integer("gameMode", 0, 3))
                .executes(this::executeCommand));
    }

    @Override
    protected int execute(CommandContext<CommandSourceStack> context) {
        CommandSender sender = sender(context);
        try {
            String gameModeValue = hasArgument(context, "gameMode")
                    ? argumentValue(context, "gameMode")
                    : String.valueOf(argumentValue(context, "gameModeValue", Integer.class));
            GameMode gameMode = GameMode.from(gameModeValue.toLowerCase(Locale.ROOT));

            ((CloudServer) sender.getServer()).getConfig().setGamemode(gameMode);
            sender.sendMessage(Component.translatable("commands.defaultgamemode.success", Component.translatable(gameMode)));
        } catch (IllegalArgumentException e) {
            return failure(context, Component.text("Unknown game mode"));
        }

        return success();
    }
}
