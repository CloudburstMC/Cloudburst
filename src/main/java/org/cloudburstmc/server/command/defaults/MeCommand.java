package org.cloudburstmc.server.command.defaults;

import com.nukkitx.protocol.bedrock.data.command.CommandParamType;
import org.cloudburstmc.server.command.Command;
import org.cloudburstmc.server.command.CommandSender;
import org.cloudburstmc.server.command.data.CommandData;
import org.cloudburstmc.server.command.data.CommandParameter;
import org.cloudburstmc.server.locale.TranslationContainer;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.utils.TextFormat;


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

        String name;
        if (sender instanceof Player) {
            name = ((Player) sender).getDisplayName();
        } else {
            name = sender.getName();
        }

        String msg = String.join(" ", args);
        sender.getServer().broadcastMessage(new TranslationContainer("chat.type.emote", name, TextFormat.WHITE + msg));

        return true;
    }
}
