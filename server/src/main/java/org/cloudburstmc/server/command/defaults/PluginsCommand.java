package org.cloudburstmc.server.command.defaults;

import com.mojang.brigadier.context.CommandContext;
import net.kyori.adventure.text.Component;
import org.cloudburstmc.api.command.CommandSender;
import org.cloudburstmc.api.command.CommandSourceStack;
import org.cloudburstmc.api.plugin.PluginContainer;
import org.cloudburstmc.server.command.AdvertisedCommand;

import java.util.Collection;
import java.util.List;

public class PluginsCommand extends AdvertisedCommand {

    public PluginsCommand() {
        super("plugins", "cloudburst.command.plugins.description", List.of("pl"),
                "cloudburst.command.plugins");
    }

    @Override
    protected int execute(CommandContext<CommandSourceStack> context) {
        CommandSender sender = sender(context);
        this.sendPluginList(sender);
        return success();
    }

    private void sendPluginList(CommandSender sender) {
        Collection<PluginContainer> plugins = sender.getServer().getPluginManager().getAllPlugins();

        Component list = Component.empty();
        boolean first = true;
        for (PluginContainer plugin : plugins) {
            if (!first) {
                list = list.append(Component.text(", "));
            }
            list = list.append(Component.text(plugin.getDescription().getName()));
            first = false;
        }

        sender.sendMessage(Component.translatable("cloudburst.command.plugins.success",
                Component.text(plugins.size()), list));
    }
}
