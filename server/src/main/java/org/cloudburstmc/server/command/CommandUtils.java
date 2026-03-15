package org.cloudburstmc.server.command;

import lombok.experimental.UtilityClass;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.cloudburstmc.api.command.CommandSender;
import org.cloudburstmc.api.permission.Permissible;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.server.CloudServer;

import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

@UtilityClass
public class CommandUtils {
    private static final Pattern RELATIVE_PATTERN = Pattern.compile("(~)?([+\\-]?[0-9]+\\.?[0-9]*)?");

    public static Optional<Vector3f> parseVector3f(String[] args, Vector3f relative) {
        checkNotNull(args, "args");
        if (args.length < 3) {
            return Optional.empty();
        }

        try {
            return Optional.of(Vector3f.from(
                    getPosition(args[0], relative.getX()),
                    getPosition(args[1], relative.getY()),
                    getPosition(args[2], relative.getZ())
            ));
        } catch (IllegalArgumentException e) {
            // ignore
        }
        return Optional.empty();
    }

    public static float getPosition(String pos, float relative) throws IllegalArgumentException {
        Matcher matcher = RELATIVE_PATTERN.matcher(pos);
        checkArgument(matcher.matches(), "Invalid position");
        float position;

        if (matcher.group(2) != null) {
            position = Float.parseFloat(matcher.group(2));
        } else {
            position = 0;
        }

        if (matcher.group(1) != null) {
            position += relative;
        }
        return position;
    }

    public static void broadcastCommandMessage(CommandSender source, Component message) {
        broadcastCommandMessage(source, message, true);
    }

    public static void broadcastCommandMessage(CommandSender source, Component message, boolean sendToSource) {
        Set<Permissible> users = source.getServer().getPermissionManager().getPermissionSubscriptions(CloudServer.BROADCAST_CHANNEL_ADMINISTRATIVE);

        Component adminMessage = Component.text("[")
                .append(source.name())
                .append(Component.text(": "))
                .append(message)
                .append(Component.text("]"));
        Component coloredMessage = adminMessage.color(NamedTextColor.GRAY).decorate(TextDecoration.ITALIC);

        if (sendToSource && !(source instanceof ConsoleCommandSender)) {
            source.sendMessage(message);
        }

        for (Permissible user : users) {
            if (user instanceof CommandSender commandSender) {
                if (user instanceof ConsoleCommandSender) {
                    commandSender.sendMessage(adminMessage);
                } else if (!user.equals(source)) {
                    commandSender.sendMessage(coloredMessage);
                }
            }
        }
    }
}
