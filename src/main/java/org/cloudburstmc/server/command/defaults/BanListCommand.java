package org.cloudburstmc.server.command.defaults;

import org.cloudburstmc.server.command.Command;
import org.cloudburstmc.server.command.CommandSender;
import org.cloudburstmc.server.command.data.CommandData;
import org.cloudburstmc.server.command.data.CommandParameter;
import org.cloudburstmc.server.locale.TranslationContainer;
import org.cloudburstmc.server.permission.BanEntry;
import org.cloudburstmc.server.permission.BanList;

import java.util.Iterator;

/**
 * Created on 2015/11/11 by xtypr.
 * Package cn.nukkit.command.defaults in project Nukkit .
 */
public class BanListCommand extends Command {
    public BanListCommand() {
        super("banlist", CommandData.builder("banlist")
                .setPermissions("cloudburst.command.ban.list")
                .setUsageMessage("/banlist <ips|players>")
                .setParameters(new CommandParameter[]{new CommandParameter("ips|players", true)})
                .build());
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        if (!this.testPermission(sender)) {
            return true;
        }

        BanList list;
        boolean ips = false;
        if (args.length > 0) {
            switch (args[0].toLowerCase()) {
                case "ips":
                    list = sender.getServer().getIPBans();
                    ips = true;
                    break;
                case "players":
                    list = sender.getServer().getNameBans();
                    break;
                default:
                    return false;
            }
        } else {
            list = sender.getServer().getNameBans();
        }

        StringBuilder builder = new StringBuilder();
        Iterator<BanEntry> itr = list.getEntires().values().iterator();
        while (itr.hasNext()) {
            builder.append(itr.next().getName());
            if (itr.hasNext()) {
                builder.append(", ");
            }
        }

        if (ips) {
            sender.sendMessage(new TranslationContainer("commands.banlist.ips", list.getEntires().size()));
        } else {
            sender.sendMessage(new TranslationContainer("commands.banlist.players", list.getEntires().size()));
        }
        sender.sendMessage(builder.toString());
        return true;
    }
}
