package org.cloudburstmc.server.command.defaults;

import org.cloudburstmc.api.command.CommandSender;
import org.cloudburstmc.server.command.Command;
import org.cloudburstmc.server.command.ConsoleCommandSender;
import org.cloudburstmc.server.command.data.CommandData;
import org.cloudburstmc.server.command.data.CommandParameter;
import org.cloudburstmc.server.locale.TranslationContainer;
import org.cloudburstmc.server.player.CloudPlayer;
import org.cloudburstmc.server.utils.TextFormat;


/**
 * Created on 2015/11/12 by xtypr.
 * Package cn.nukkit.command.defaults in project Nukkit .
 */
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

        String senderString;
        if (sender instanceof CloudPlayer) {
            senderString = ((CloudPlayer) sender).getDisplayName();
        } else if (sender instanceof ConsoleCommandSender) {
            senderString = "Server";
        } else {
            senderString = sender.getName();
        }

        String msg = String.join(" ", args);

        sender.getServer().broadcastMessage(new TranslationContainer(
                TextFormat.LIGHT_PURPLE + "%chat.type.announcement",
                senderString, TextFormat.LIGHT_PURPLE + msg));
        return true;
    }
}
