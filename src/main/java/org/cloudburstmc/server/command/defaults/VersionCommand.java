package org.cloudburstmc.server.command.defaults;

import lombok.val;
import org.cloudburstmc.server.command.Command;
import org.cloudburstmc.server.command.CommandSender;
import org.cloudburstmc.server.command.data.CommandData;
import org.cloudburstmc.server.locale.TranslationContainer;
import org.cloudburstmc.server.network.ProtocolInfo;
import org.cloudburstmc.server.plugin.PluginContainer;
import org.cloudburstmc.server.utils.TextFormat;

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

            val exactPlugin = sender.getServer().getPluginManager().getPlugin(pluginName.toString()).orElseGet(() -> {
                final String finalPluginName = pluginName.toString().toLowerCase();
                for (PluginContainer container : sender.getServer().getPluginManager().getAllPlugins()) {
                    if (container.getName().toLowerCase().contains(finalPluginName)) {
                        return container;
                    }
                }

                return null;
            });

            if (exactPlugin != null) {
                sender.sendMessage(TextFormat.DARK_GREEN + exactPlugin.getName() + TextFormat.WHITE + " version " + TextFormat.DARK_GREEN + exactPlugin.getVersion());
                exactPlugin.getDescription().ifPresent((desc) -> {
                    sender.sendMessage(desc);
                });

                exactPlugin.getUrl().ifPresent(url -> {
                    sender.sendMessage("Website: " + url);
                });

                val authors = exactPlugin.getAuthors();
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