package org.cloudburstmc.server.command.defaults;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.cloudburstmc.api.command.CommandSender;
import org.cloudburstmc.api.command.CommandSourceStack;
import org.cloudburstmc.api.command.Commands;
import org.cloudburstmc.api.command.argument.CommandArguments;
import org.cloudburstmc.api.command.argument.CommandArgumentTypes;
import org.cloudburstmc.server.CloudServer;
import org.cloudburstmc.server.command.CloudConsoleCommandSender;
import org.cloudburstmc.server.command.AdvertisedCommand;
import org.cloudburstmc.server.command.network.CommandNetworkData;
import org.cloudburstmc.server.player.CloudPlayer;

public class SayCommand extends AdvertisedCommand {

    public SayCommand() {
        super("say", "commands.say.description", CommandNetworkData.GAME_DIRECTORS_MESSAGE_NOT_CHEAT,
                "cloudburst.command.say");
    }

    @Override
    public void configure(LiteralArgumentBuilder<CommandSourceStack> builder, String label, CommandArguments arguments) {
        builder.then(Commands.argument("message", CommandArgumentTypes.message())
                .executes(this::executeCommand));
    }

    @Override
    protected int execute(CommandContext<CommandSourceStack> context) {
        CommandSender sender = sender(context);
        Component senderName;
        if (sender instanceof CloudPlayer) {
            senderName = ((CloudPlayer) sender).displayName();
        } else if (sender instanceof CloudConsoleCommandSender) {
            senderName = Component.text("Server");
        } else {
            senderName = sender.name();
        }

        String msg = argumentValue(context, "message");

        ((CloudServer) sender.getServer()).broadcastMessage(
                Component.translatable("chat.type.announcement", senderName, Component.text(msg))
                        .color(NamedTextColor.LIGHT_PURPLE));
        return success();
    }
}
