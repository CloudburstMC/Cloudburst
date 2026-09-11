package org.cloudburstmc.server.command.defaults;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.cloudburstmc.api.command.CommandSender;
import org.cloudburstmc.api.command.CommandSourceStack;
import org.cloudburstmc.api.command.Commands;
import org.cloudburstmc.api.command.argument.CommandArguments;
import org.cloudburstmc.api.command.argument.CommandArgumentTypes;
import org.cloudburstmc.api.plugin.PluginContainer;
import org.cloudburstmc.api.plugin.PluginDescription;
import org.cloudburstmc.server.command.AdvertisedCommand;
import org.cloudburstmc.server.network.ProtocolInfo;

import java.util.List;
import java.util.Locale;

public class VersionCommand extends AdvertisedCommand {

    public VersionCommand() {
        super("version", "cloudburst.command.version.description", List.of("ver", "about"),
                "cloudburst.command.version");
    }

    @Override
    public void configure(LiteralArgumentBuilder<CommandSourceStack> builder, String label, CommandArguments arguments) {
        builder.executes(this::executeCommand);
        builder.then(Commands.argument("plugin", CommandArgumentTypes.text())
                .executes(this::executeCommand));
    }

    @Override
    protected int execute(CommandContext<CommandSourceStack> context) {
        CommandSender sender = sender(context);
        if (!hasArgument(context, "plugin")) {
            sender.sendMessage(Component.translatable("cloudburst.server.info.extended",
                    Component.text(sender.getServer().getName()),
                    Component.text(sender.getServer().getImplementationVersion()),
                    Component.text(sender.getServer().getApiVersion()),
                    Component.text(sender.getServer().getVersion()),
                    Component.text(ProtocolInfo.getDefaultProtocolVersion())));
        } else {
            String pluginName = argumentValue(context, "plugin");
            PluginContainer exactPlugin = sender.getServer().getPluginManager().getPlugin(pluginName).orElseGet(() -> {
                final String finalPluginName = pluginName.toLowerCase(Locale.ROOT);
                for (PluginContainer container : sender.getServer().getPluginManager().getAllPlugins()) {
                    if (container.getDescription().getName().toLowerCase(Locale.ROOT).contains(finalPluginName)) {
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

        return success();
    }
}
