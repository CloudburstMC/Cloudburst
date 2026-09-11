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
import org.cloudburstmc.api.event.player.PlayerKickEvent;
import org.cloudburstmc.server.command.CommandUtils;
import org.cloudburstmc.server.command.AdvertisedCommand;
import org.cloudburstmc.server.command.network.CommandNetworkData;
import org.cloudburstmc.server.player.CloudPlayer;

import java.util.List;

public class KickCommand extends AdvertisedCommand {

    public KickCommand() {
        super("kick", "commands.kick.description", CommandNetworkData.GAME_DIRECTORS_NOT_CHEAT,
                "cloudburst.command.kick");
    }

    @Override
    public void configure(LiteralArgumentBuilder<CommandSourceStack> builder, String label, CommandArguments arguments) {
        builder.then(Commands.argument("player", arguments.players())
                .executes(this::executeCommand)
                .then(Commands.argument("reason", CommandArgumentTypes.message())
                        .executes(this::executeCommand)));
    }

    @Override
    protected int execute(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSender sender = sender(context);
        String reason = hasArgument(context, "reason") ? argumentValue(context, "reason") : "";

        List<CloudPlayer> players = cloudPlayersArgument(context, "player");
        if (players.isEmpty()) {
            return success();
        }

        for (CloudPlayer player : players) {
            player.kick(PlayerKickEvent.Reason.KICKED_BY_ADMIN, reason);
            if (!reason.isBlank()) {
                CommandUtils.broadcastCommandMessage(sender, Component.translatable("commands.kick.success.reason",
                        Component.text(player.getName()), Component.text(reason)));
            } else {
                CommandUtils.broadcastCommandMessage(sender, Component.translatable("commands.kick.success",
                        Component.text(player.getName())));
            }
        }

        return success();
    }
}
