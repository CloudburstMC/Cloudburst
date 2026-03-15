package org.cloudburstmc.server.command.defaults;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.cloudburstmc.api.command.CommandSender;
import org.cloudburstmc.server.CloudServer;
import org.cloudburstmc.server.command.Command;
import org.cloudburstmc.server.command.ConsoleCommandSender;
import org.cloudburstmc.server.command.data.CommandData;
import org.cloudburstmc.server.command.data.CommandParameter;
import org.cloudburstmc.server.player.CloudPlayer;

public class SayCommand extends Command {

    public SayCommand() {
        super("say", CommandData.builder("say")
                .setDescription("commands.say.description")
                .setUsageMessage("/say <usage>")
                .setPermissions("cloudburst.command.say")
                .setParameters(new CommandParameter[]{
                        new CommandParameter("message")
                })
                .build());
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        if (!this.testPermission(sender)) {
            return true;
        }

        if (args.length == 0) {
            return false;
        }

        Component senderName;
        if (sender instanceof CloudPlayer) {
            senderName = ((CloudPlayer) sender).displayName();
        } else if (sender instanceof ConsoleCommandSender) {
            senderName = Component.text("Server");
        } else {
            senderName = sender.name();
        }

        String msg = String.join(" ", args);

        ((CloudServer) sender.getServer()).broadcastMessage(
                Component.translatable("chat.type.announcement", senderName, Component.text(msg))
                        .color(NamedTextColor.LIGHT_PURPLE));
        return true;
    }
}
