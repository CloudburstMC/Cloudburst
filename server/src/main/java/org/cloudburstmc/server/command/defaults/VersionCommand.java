package org.cloudburstmc.server.command.defaults;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.cloudburstmc.api.command.CommandSender;
import org.cloudburstmc.api.plugin.PluginContainer;
import org.cloudburstmc.api.plugin.PluginDescription;
import org.cloudburstmc.server.command.Command;
import org.cloudburstmc.server.command.data.CommandData;
import org.cloudburstmc.server.network.ProtocolInfo;

import java.util.List;
import java.util.StringJoiner;

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
            sender.sendMessage(Component.translatable("cloudburst.server.info.extended",
                    Component.text(sender.getServer().getName()),
                    Component.text(sender.getServer().getImplementationVersion()),
                    Component.text(sender.getServer().getApiVersion()),
                    Component.text(sender.getServer().getVersion()),
                    Component.text(ProtocolInfo.getDefaultProtocolVersion())));
        } else {
            StringJoiner pluginName = new StringJoiner(" ");
            for (String arg : args) pluginName.add(arg);

            var exactPlugin = sender.getServer().getPluginManager().getPlugin(pluginName.toString()).orElseGet(() -> {
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
                sender.sendMessage(Component.text(description.getName()).color(NamedTextColor.DARK_GREEN)
                        .append(Component.text(" version ").color(NamedTextColor.WHITE))
                        .append(Component.text(description.getVersion()).color(NamedTextColor.DARK_GREEN)));
                description.getDescription().ifPresent(d -> sender.sendMessage(Component.text(d)));
                description.getUrl().ifPresent(url -> sender.sendMessage(Component.text("Website: " + url)));

                List<String> authors = description.getAuthors();
                if (authors.size() == 1) {
                    sender.sendMessage(Component.text("Author: " + authors.get(0)));
                } else if (authors.size() >= 2) {
                    sender.sendMessage(Component.text("Authors: " + String.join(", ", authors)));
                }
            } else {
                sender.sendMessage(Component.translatable("cloudburst.command.version.noSuchPlugin"));
            }
        }
        return true;
    }
}
