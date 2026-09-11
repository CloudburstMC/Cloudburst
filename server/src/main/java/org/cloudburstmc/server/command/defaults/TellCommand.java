package org.cloudburstmc.server.command.defaults;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.cloudburstmc.api.command.CommandSender;
import org.cloudburstmc.api.command.CommandSourceStack;
import org.cloudburstmc.api.command.Commands;
import org.cloudburstmc.api.command.argument.CommandArguments;
import org.cloudburstmc.api.command.argument.CommandArgumentTypes;
import org.cloudburstmc.server.command.AdvertisedCommand;
import org.cloudburstmc.server.command.network.CommandNetworkData;
import org.cloudburstmc.server.player.CloudPlayer;

import java.util.List;

public class TellCommand extends AdvertisedCommand {

    public TellCommand() {
        super("tell", "commands.tell.description", List.of("w", "msg"),
                CommandNetworkData.ANY_MESSAGE_NOT_CHEAT,
                "cloudburst.command.tell");
    }

    @Override
    public void configure(LiteralArgumentBuilder<CommandSourceStack> builder, String label, CommandArguments arguments) {
        builder.then(Commands.argument("player", arguments.players())
                .then(Commands.argument("message", CommandArgumentTypes.message())
                        .executes(this::executeCommand)));
    }

    @Override
    protected int execute(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSender sender = sender(context);
        List<CloudPlayer> players = cloudPlayersArgument(context, "player");
        if (players.isEmpty()) {
            sender.sendMessage(Component.translatable("commands.generic.player.notFound"));
            return success();
        }

        String msg = argumentValue(context, "message");
        Component senderDisplayName = (sender instanceof CloudPlayer) ? ((CloudPlayer) sender).displayName() : sender.name();
        for (CloudPlayer player : players) {
            if (player.equals(sender)) {
                sender.sendMessage(Component.translatable("commands.message.sameTarget").color(NamedTextColor.RED));
                continue;
            }

            sender.sendMessage(Component.translatable("commands.message.display.outgoing",
                    player.displayName(), Component.text(msg)));
            player.sendMessage(Component.translatable("commands.message.display.incoming",
                    senderDisplayName, Component.text(msg)));
        }

        return success();
    }
}
