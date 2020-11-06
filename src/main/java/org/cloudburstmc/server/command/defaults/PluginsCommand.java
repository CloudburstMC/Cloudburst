package org.cloudburstmc.server.command.defaults;

import lombok.val;
import org.cloudburstmc.api.plugin.PluginContainer;
import org.cloudburstmc.server.command.Command;
import org.cloudburstmc.server.command.CommandSender;
import org.cloudburstmc.server.command.data.CommandData;
import org.cloudburstmc.server.locale.TranslationContainer;
import org.cloudburstmc.server.utils.TextFormat;

/**
 * Created on 2015/11/12 by xtypr.
 * Package cn.nukkit.command.defaults in project Nukkit .
 */
public class PluginsCommand extends Command {

    public PluginsCommand() {
        super("plugins", CommandData.builder("plugins")
                .setDescription("%cloudburst.command.plugins.description")
                .setUsageMessage("%cloudburst.command.plugins.usage")
                .setAliases("pl")
                .setPermissions("cloudburst.command.plugins")
                .build());
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        if (!this.testPermission(sender)) {
            return true;
        }

        this.sendPluginList(sender);
        return true;
    }

    private void sendPluginList(CommandSender sender) {
        StringBuilder list = new StringBuilder();
        val plugins = sender.getServer().getPluginManager().getAllPlugins();
        for (PluginContainer plugin : plugins) {
            if (list.length() > 0) {
                list.append(TextFormat.WHITE);
                list.append(", ");
            }
            list.append(plugin.getDescription().getName());
        }

        sender.sendMessage(new TranslationContainer("cloudburst.command.plugins.success", String.valueOf(plugins.size()), list.toString()));
    }
}