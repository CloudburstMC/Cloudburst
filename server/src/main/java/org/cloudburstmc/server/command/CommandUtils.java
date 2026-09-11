package org.cloudburstmc.server.command;

import lombok.experimental.UtilityClass;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.cloudburstmc.api.command.CommandSender;
import org.cloudburstmc.api.player.OfflinePlayer;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.server.CloudServer;

import java.util.Collection;

@UtilityClass
public class CommandUtils {
    public static String displayName(OfflinePlayer player, String fallback) {
        String name = player.getName();
        return name == null || name.isBlank() ? fallback : name;
    }

    public static void sendUsage(CommandSender sender, Collection<String> usages) {
        for (String usage : usages) {
            sender.sendMessage(Component.translatable("commands.generic.usage", Component.text(usage)));
        }
    }

    public static void broadcastCommandMessage(CommandSender source, Component message) {
        broadcastCommandMessage(source, message, true);
    }

    public static void broadcastCommandMessage(CommandSender source, Component message, boolean sendToSource) {
        Component adminMessage = Component.text("[")
                .append(source.name())
                .append(Component.text(": "))
                .append(message)
                .append(Component.text("]"));
        Component coloredMessage = adminMessage.color(NamedTextColor.GRAY).decorate(TextDecoration.ITALIC);

        if (sendToSource && !(source instanceof CloudConsoleCommandSender)) {
            source.sendMessage(message);
        }

        CloudServer server = (CloudServer) source.getServer();
        if (source != server.getConsoleSender()
                && server.getConsoleSender().hasPermission(CloudServer.BROADCAST_CHANNEL_ADMINISTRATIVE)) {
            server.getConsoleSender().sendMessage(adminMessage);
        }

        for (Player player : server.getOnlinePlayers().values()) {
            if (player != source && player.hasPermission(CloudServer.BROADCAST_CHANNEL_ADMINISTRATIVE)) {
                player.sendMessage(coloredMessage);
            }
        }
    }
}
