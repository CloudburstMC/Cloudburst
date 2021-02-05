package org.cloudburstmc.server.command.defaults;

import com.nukkitx.protocol.bedrock.data.command.CommandParamType;
import org.cloudburstmc.api.command.CommandSender;
import org.cloudburstmc.server.command.Command;
import org.cloudburstmc.server.command.data.CommandData;
import org.cloudburstmc.server.command.data.CommandParameter;
import org.cloudburstmc.server.locale.TranslationContainer;
import org.cloudburstmc.server.player.CloudPlayer;
import org.cloudburstmc.server.utils.TextFormat;

import java.util.Objects;
import java.util.StringJoiner;

/**
 * Created on 2015/11/12 by xtypr.
 * Package cn.nukkit.command.defaults in project Nukkit .
 */
public class TellCommand extends Command {

    public TellCommand() {
        super("tell", CommandData.builder("tell")
                .setDescription("commands.tell.description")
                .setUsageMessage("/tell <player> <message>")
                .setAliases("w", "msg")
                .setPermissions("cloudburst.command.tell")
                .setParameters(new CommandParameter[]{
                        new CommandParameter("player", CommandParamType.TARGET, false),
                        new CommandParameter("message")
                })
                .build());
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        if (!this.testPermission(sender)) {
            return true;
        }

        if (args.length < 2) {
            return false;
        }

        String name = args[0].toLowerCase();

        CloudPlayer player = sender.getServer().getPlayer(name);
        if (player == null) {
            sender.sendMessage(new TranslationContainer("commands.generic.player.notFound"));
            return true;
        }

        if (Objects.equals(player, sender)) {
            sender.sendMessage(new TranslationContainer(TextFormat.RED + "%commands.message.sameTarget"));
            return true;
        }

        StringJoiner msg = new StringJoiner(" ");
        for (int i = 1; i < args.length; i++) {
            msg.add(args[i]);
        }

        String displayName = (sender instanceof CloudPlayer ? ((CloudPlayer) sender).getDisplayName() : sender.getName());

        sender.sendMessage(new TranslationContainer("commands.message.display.outgoing", player.getDisplayName(), msg));
        player.sendMessage(new TranslationContainer("commands.message.display.incoming", displayName, msg));

        return true;
    }
}
