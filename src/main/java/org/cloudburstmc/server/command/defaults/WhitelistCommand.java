package org.cloudburstmc.server.command.defaults;

import com.nukkitx.protocol.bedrock.data.command.CommandParamType;
import org.cloudburstmc.server.command.Command;
import org.cloudburstmc.server.command.CommandSender;
import org.cloudburstmc.server.command.CommandUtils;
import org.cloudburstmc.server.command.data.CommandData;
import org.cloudburstmc.server.command.data.CommandParameter;
import org.cloudburstmc.server.locale.TranslationContainer;
import org.cloudburstmc.server.utils.TextFormat;

import java.util.StringJoiner;

/**
 * Created on 2015/11/12 by xtypr.
 * Package cn.nukkit.command.defaults in project Nukkit .
 */
public class WhitelistCommand extends Command {

    public WhitelistCommand() {
        super("whitelist", CommandData.builder("whitelist")
                .setDescription("commands.whitelist.description")
                .setUsageMessage("/whitelist <on|off|reload|list>\n/whitelist <add|remove> <player>")
                .setPermissions("cloudburst.command.whitelist.reload",
                        "cloudburst.command.whitelist.enable",
                        "cloudburst.command.whitelist.disable",
                        "cloudburst.command.whitelist.list",
                        "cloudburst.command.whitelist.add",
                        "cloudburst.command.whitelist.remove")
                .setParameters(new CommandParameter[]{
                        new CommandParameter("on|off|list|reload", CommandParamType.STRING, false)
                }, new CommandParameter[]{
                        new CommandParameter("add|remove", CommandParamType.STRING, false),
                        new CommandParameter("player", CommandParamType.TARGET, false)
                })
                .build());
    }


    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        if (!this.testPermission(sender)) {
            return true;
        }

        if (args.length == 0 || args.length > 2) {
            return false;
        }

        if (args.length == 1) {
            if (this.badPerm(sender, args[0].toLowerCase())) {
                return false;
            }
            switch (args[0].toLowerCase()) {
                case "reload":
                    sender.getServer().reloadWhitelist();
                    CommandUtils.broadcastCommandMessage(sender, new TranslationContainer("%commands.whitelist.reloaded"));

                    return true;
                case "on":
                    sender.getServer().getServerConfig().getPropertiesConfig().setWhitelist(true);
                    CommandUtils.broadcastCommandMessage(sender, new TranslationContainer("%commands.whitelist.enabled"));

                    return true;
                case "off":
                    sender.getServer().getServerConfig().getPropertiesConfig().setWhitelist(false);
                    CommandUtils.broadcastCommandMessage(sender, new TranslationContainer("%commands.whitelist.disabled"));

                    return true;
                case "list":
                    StringJoiner result = new StringJoiner(", ");
                    int count = 0;
                    for (String player : sender.getServer().getWhitelist().getAll().keySet()) {
                        result.add(player);
                        ++count;
                    }
                    sender.sendMessage(new TranslationContainer("commands.whitelist.list", count, count));
                    sender.sendMessage(result.toString());

                    return true;
                case "add":
                case "remove":
                    return false;
            }
        } else {
            if (this.badPerm(sender, args[0].toLowerCase())) {
                return false;
            }
            switch (args[0].toLowerCase()) {
                case "add":
                    sender.getServer().getOfflinePlayer(args[1]).setWhitelisted(true);
                    CommandUtils.broadcastCommandMessage(sender, new TranslationContainer("%commands.whitelist.add.success", args[1]));

                    return true;
                case "remove":
                    sender.getServer().getOfflinePlayer(args[1]).setWhitelisted(false);
                    CommandUtils.broadcastCommandMessage(sender, new TranslationContainer("%commands.whitelist.remove.success", args[1]));

                    return true;
            }
        }

        return true;
    }

    private boolean badPerm(CommandSender sender, String perm) {
        if (!sender.hasPermission("cloudburst.command.whitelist" + perm)) {
            sender.sendMessage(new TranslationContainer(TextFormat.RED + "%commands.generic.permission"));

            return true;
        }

        return false;
    }
}
