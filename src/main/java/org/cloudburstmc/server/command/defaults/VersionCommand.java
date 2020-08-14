package org.cloudburstmc.server.command.defaults;

import org.cloudburstmc.server.command.Command;
import org.cloudburstmc.server.command.CommandSender;
import org.cloudburstmc.server.command.data.CommandData;
import org.cloudburstmc.server.locale.TranslationContainer;
import org.cloudburstmc.server.network.ProtocolInfo;
import org.cloudburstmc.server.plugin.Plugin;
import org.cloudburstmc.server.plugin.PluginDescription;
import org.cloudburstmc.server.utils.TextFormat;

import java.util.List;
import java.util.StringJoiner;

/**
 * Created on 2015/11/12 by xtypr.
 * Package cn.nukkit.command.defaults in project Nukkit .
 */
public class VersionCommand extends Command {

    public VersionCommand() {
        super("version", CommandData.builder("version")
                .setDescription("%cloudburst.command.version.description")
                .setAliases("ver", "about")
                .setPermissions("cloudburst.command.version")
                .build());
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        if (!this.testPermission(sender)) {
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage(new TranslationContainer("cloudburst.server.info.extended", sender.getServer().getName(),
                    sender.getServer().getNukkitVersion(),
                    sender.getServer().getApiVersion(),
                    sender.getServer().getVersion(),
                    String.valueOf(ProtocolInfo.getDefaultProtocolVersion())));
        } else {
            StringJoiner pluginName = new StringJoiner(" ");
            for (String arg : args) pluginName.add(arg);

            final boolean[] found = {false};
            final Plugin[] exactPlugin = {sender.getServer().getPluginManager().getPlugin(pluginName.toString())};

            if (exactPlugin[0] == null) {
                final String finalPluginName = pluginName.toString().toLowerCase();
                sender.getServer().getPluginManager().getPlugins().forEach((s, p) -> {
                    if (s.toLowerCase().contains(finalPluginName)) {
                        exactPlugin[0] = p;
                        found[0] = true;
                    }
                });
            } else {
                found[0] = true;
            }

            if (found[0]) {
                PluginDescription desc = exactPlugin[0].getDescription();
                sender.sendMessage(TextFormat.DARK_GREEN + desc.getName() + TextFormat.WHITE + " version " + TextFormat.DARK_GREEN + desc.getVersion());
                if (desc.getDescription() != null) {
                    sender.sendMessage(desc.getDescription());
                }
                if (desc.getWebsite() != null) {
                    sender.sendMessage("Website: " + desc.getWebsite());
                }
                List<String> authors = desc.getAuthors();
                final String[] authorsString = {""};
                authors.forEach((s) -> authorsString[0] += s);
                if (authors.size() == 1) {
                    sender.sendMessage("Author: " + authorsString[0]);
                } else if (authors.size() >= 2) {
                    sender.sendMessage("Authors: " + authorsString[0]);
                }
            } else {
                sender.sendMessage(new TranslationContainer("cloudburst.command.version.noSuchPlugin"));
            }
        }
        return true;
    }
}