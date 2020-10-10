package org.cloudburstmc.server.command.defaults;

import lombok.val;
import org.cloudburstmc.api.plugin.PluginContainer;
import org.cloudburstmc.api.plugin.PluginDescription;
import org.cloudburstmc.server.command.Command;
import org.cloudburstmc.server.command.CommandSender;
import org.cloudburstmc.server.command.data.CommandData;
import org.cloudburstmc.server.locale.TranslationContainer;
import org.cloudburstmc.server.network.ProtocolInfo;
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
                    sender.getServer().getImplementationVersion(),
                    sender.getServer().getApiVersion(),
                    sender.getServer().getVersion(),
                    String.valueOf(ProtocolInfo.getDefaultProtocolVersion())));
        } else {
            StringJoiner pluginName = new StringJoiner(" ");
            for (String arg : args) pluginName.add(arg);

            val exactPlugin = sender.getServer().getPluginManager().getPlugin(pluginName.toString()).orElseGet(() -> {
                final String finalPluginName = pluginName.toString().toLowerCase();
                for (PluginContainer container : sender.getServer().getPluginManager().getAllPlugins()) {
                    if (container.getDescription().getName().toLowerCase().contains(finalPluginName)) {
                        return container;
                    }
                }

                return null;
            });

            if (exactPlugin != null) {
                PluginDescription description = exactPlugin.getDescription();
                sender.sendMessage(TextFormat.DARK_GREEN + description.getName() + TextFormat.WHITE + " version " + TextFormat.DARK_GREEN + description.getVersion());
                description.getDescription().ifPresent(sender::sendMessage);

                description.getUrl().ifPresent(url -> sender.sendMessage("Website: " + url));

                List<String> authors = description.getAuthors();

                if (authors.size() == 1) {
                    sender.sendMessage("Author: " + authors.get(0));
                } else if (authors.size() >= 2) {
                    sender.sendMessage("Authors: " + String.join(", ", authors));
                }
            } else {
                sender.sendMessage(new TranslationContainer("cloudburst.command.version.noSuchPlugin"));
            }
        }
        return true;
    }
}