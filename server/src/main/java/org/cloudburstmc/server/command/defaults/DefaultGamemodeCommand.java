package org.cloudburstmc.server.command.defaults;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.kyori.adventure.text.Component;
import org.cloudburstmc.api.command.CommandSender;
import org.cloudburstmc.api.command.CommandSourceStack;
import org.cloudburstmc.api.command.Commands;
import org.cloudburstmc.api.command.argument.CommandArgumentTypes;
import org.cloudburstmc.api.command.argument.CommandArguments;
import org.cloudburstmc.api.player.GameMode;
import org.cloudburstmc.server.command.AdvertisedCommand;
import org.cloudburstmc.server.command.argument.GameModeArguments;


public class DefaultGamemodeCommand extends AdvertisedCommand {

    public DefaultGamemodeCommand() {
        super("defaultgamemode", "commands.defaultgamemode.description", GamemodeCommand.NETWORK_DATA, "cloudburst.command.defaultgamemode");
    }

    @Override
    public void configure(LiteralArgumentBuilder<CommandSourceStack> builder, String label, CommandArguments arguments) {
        builder.then(Commands.argument("gameMode", CommandArgumentTypes.fixedEnumNamed("gameMode", "DefaultGameMode",
                        "creative", "spectator", "survival", "adventure", "c", "s", "a"))
                .executes(this::executeCommand));
        builder.then(Commands.argument("gameModeValue", CommandArgumentTypes.integer("gameMode"))
                .executes(this::executeCommand));
    }

    @Override
    protected int execute(CommandContext<CommandSourceStack> context) {
        CommandSender sender = sender(context);
        String input = hasArgument(context, "gameMode")
                ? argumentValue(context, "gameMode")
                : String.valueOf(argumentValue(context, "gameModeValue", Integer.class));
        GameMode gameMode = GameModeArguments.gameMode(input);
        if (gameMode == null) {
            return failure(context, Component.translatable("commands.gamemode.fail.invalid", Component.text(input)));
        }

        sender.getServer().setDefaultGameMode(gameMode);
        sender.sendMessage(Component.translatable("commands.defaultgamemode.success", Component.translatable(gameMode)));

        return success();
    }
}
