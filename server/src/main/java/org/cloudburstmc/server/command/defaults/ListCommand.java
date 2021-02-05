package org.cloudburstmc.server.command.defaults;

import org.cloudburstmc.api.command.CommandSender;
import org.cloudburstmc.server.command.Command;
import org.cloudburstmc.server.command.data.CommandData;
import org.cloudburstmc.server.locale.TranslationContainer;
import org.cloudburstmc.server.player.CloudPlayer;

import java.util.StringJoiner;

/**
 * Created on 2015/11/11 by xtypr.
 * Package cn.nukkit.command.defaults in project Nukkit .
 */
public class ListCommand extends Command {

    public ListCommand() {
        super("list", CommandData.builder("list")
                .setDescription("commands.list.description")
                .setUsageMessage("/list")
                .setPermissions("cloudburst.command.list")
                .build());
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        if (!this.testPermission(sender)) {
            return true;
        }
        StringJoiner online = new StringJoiner(", ");
        int onlineCount = 0;
        for (CloudPlayer player : sender.getServer().getOnlinePlayers().values()) {
            if (player.isOnline() && (!(sender instanceof CloudPlayer) || ((CloudPlayer) sender).canSee(player))) {
                online.add(player.getDisplayName());
                ++onlineCount;
            }
        }

        sender.sendMessage(new TranslationContainer("commands.players.list",
                onlineCount, sender.getServer().getMaxPlayers()));
        sender.sendMessage(online.toString());
        return true;
    }
}
