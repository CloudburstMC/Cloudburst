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
import org.cloudburstmc.server.command.AdvertisedCommand;
import org.cloudburstmc.server.command.network.CommandNetworkData;
import org.cloudburstmc.server.player.CloudPlayer;

import java.util.List;

public class TitleCommand extends AdvertisedCommand {
    public TitleCommand() {
        super("title", "commands.title.description", CommandNetworkData.GAME_DIRECTORS_MESSAGE,
                "cloudburst.command.title");
    }

    @Override
    public void configure(LiteralArgumentBuilder<CommandSourceStack> builder, String label, CommandArguments arguments) {
        builder.then(Commands.argument("player", arguments.players())
                .then(Commands.argument("clear", CommandArgumentTypes.fixedEnumNamed("clear", "TitleClear", "clear"))
                        .executes(this::executeCommand))
                .then(Commands.argument("reset", CommandArgumentTypes.fixedEnumNamed("reset", "TitleReset", "reset"))
                        .executes(this::executeCommand))
                .then(Commands.argument("title", CommandArgumentTypes.fixedEnumNamed("title", "TitleTitle", "title"))
                        .then(Commands.argument("titleText", CommandArgumentTypes.message())
                                .executes(this::executeCommand)))
                .then(Commands.argument("subtitle", CommandArgumentTypes.fixedEnumNamed("subtitle", "TitleSubtitle", "subtitle"))
                        .then(Commands.argument("subtitleText", CommandArgumentTypes.message("titleText"))
                                .executes(this::executeCommand)))
                .then(Commands.argument("actionbar", CommandArgumentTypes.fixedEnumNamed("actionbar", "TitleActionbar", "actionbar"))
                        .then(Commands.argument("actionbarText", CommandArgumentTypes.message("titleText"))
                                .executes(this::executeCommand)))
                .then(Commands.argument("times", CommandArgumentTypes.fixedEnumNamed("times", "TitleTimes", "times"))
                        .then(Commands.argument("fadeIn", CommandArgumentTypes.integer(0))
                                .then(Commands.argument("stay", CommandArgumentTypes.integer(0))
                                        .then(Commands.argument("fadeOut", CommandArgumentTypes.integer(0))
                                                .executes(this::executeCommand))))));
    }

    @Override
    protected int execute(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSender sender = sender(context);
        List<CloudPlayer> players = cloudPlayersArgument(context, "player");
        if (players.isEmpty()) {
            return success();
        }

        if (hasArgument(context, "clear")) {
            for (CloudPlayer player : players) {
                player.clearTitle();
                sender.sendMessage(Component.translatable("cloudburst.command.title.clear",
                        Component.text(player.getName())));
            }
        } else if (hasArgument(context, "reset")) {
            for (CloudPlayer player : players) {
                player.resetTitle();
                sender.sendMessage(Component.translatable("cloudburst.command.title.reset",
                        Component.text(player.getName())));
            }
        } else if (hasArgument(context, "titleText")) {
            String text = argumentValue(context, "titleText");
            for (CloudPlayer player : players) {
                player.sendTitle(Component.text(text));
                sender.sendMessage(Component.translatable("cloudburst.command.title.title",
                        Component.text(text), Component.text(player.getName())));
            }
        } else if (hasArgument(context, "subtitleText")) {
            String text = argumentValue(context, "subtitleText");
            for (CloudPlayer player : players) {
                player.sendSubtitle(Component.text(text));
                sender.sendMessage(Component.translatable("cloudburst.command.title.subtitle",
                        Component.text(text), Component.text(player.getName())));
            }
        } else if (hasArgument(context, "actionbarText")) {
            String text = argumentValue(context, "actionbarText");
            for (CloudPlayer player : players) {
                player.sendActionBar(Component.text(text));
                sender.sendMessage(Component.translatable("cloudburst.command.title.actionbar",
                        Component.text(text), Component.text(player.getName())));
            }
        } else if (hasArgument(context, "times")) {
            int fadeIn = argumentValue(context, "fadeIn", Integer.class);
            int stay = argumentValue(context, "stay", Integer.class);
            int fadeOut = argumentValue(context, "fadeOut", Integer.class);
            for (CloudPlayer player : players) {
                player.setTitleTimes(fadeIn, stay, fadeOut);
                sender.sendMessage(Component.translatable("cloudburst.command.title.times.success",
                        Component.text(fadeIn), Component.text(stay), Component.text(fadeOut),
                        Component.text(player.getName())));
            }
        } else {
            return usage();
        }

        return success();
    }
}
