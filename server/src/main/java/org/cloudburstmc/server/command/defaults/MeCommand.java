package org.cloudburstmc.server.command.defaults;

import net.kyori.adventure.text.Component;
import org.cloudburstmc.protocol.bedrock.data.command.CommandParamType;
import org.cloudburstmc.server.CloudServer;
import org.cloudburstmc.server.command.Command;
import org.cloudburstmc.server.command.data.CommandData;
import org.cloudburstmc.server.command.data.CommandParameter;
import org.cloudburstmc.server.player.CloudPlayer;
import org.cloudburstmc.api.command.CommandSender;


/**
 * Created on 2015/11/12 by xtypr.
 * Package cn.nukkit.command.defaults in project Nukkit .
 */
public class MeCommand extends Command {

    public MeCommand() {
        super("me", CommandData.builder("me")
                .setDescription("commands.me.description")
                .setUsageMessage("/me <action>")
                .setPermissions("cloudburst.command.me")
                .setParameters(new CommandParameter[]{
                        new CommandParameter("action ...", CommandParamType.TEXT, false)
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
        } else {
            senderName = sender.name();
        }

        String msg = String.join(" ", args);
        ((CloudServer) sender.getServer()).broadcastMessage(
                Component.translatable("chat.type.emote", senderName, Component.text(msg)));

        return true;
    }
}
