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
import org.cloudburstmc.server.command.network.CommandNetworkData;
import org.cloudburstmc.server.player.CloudPlayer;

public class MeCommand extends AdvertisedCommand {

    public MeCommand() {
        super("me", "commands.me.description", CommandNetworkData.ANY_MESSAGE_NOT_CHEAT,
                "cloudburst.command.me");
    }

    @Override
    public void configure(LiteralArgumentBuilder<CommandSourceStack> builder, String label, CommandArguments arguments) {
        builder.then(Commands.argument("action", CommandArgumentTypes.message())
                .executes(this::executeCommand));
    }

    @Override
    protected int execute(CommandContext<CommandSourceStack> context) {
        CommandSender sender = sender(context);
        Component senderName;
        if (sender instanceof CloudPlayer) {
            senderName = ((CloudPlayer) sender).displayName();
        } else {
            senderName = sender.name();
        }

        String msg = argumentValue(context, "action");
        ((CloudServer) sender.getServer()).broadcastMessage(
                Component.translatable("chat.type.emote", senderName, Component.text(msg)));

        return success();
    }
}
