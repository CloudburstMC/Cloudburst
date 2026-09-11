package org.cloudburstmc.server.command;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.command.CommandSender;
import org.cloudburstmc.api.command.CommandSourceStack;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.level.Location;

import java.util.Objects;

/**
 * Immutable server implementation of a command execution source.
 */
public record CloudCommandSourceStack(
        CommandSender sender,
        @Nullable Entity executor,
        Location location
) implements CommandSourceStack {

    public CloudCommandSourceStack {
        Objects.requireNonNull(sender, "sender");
        Objects.requireNonNull(location, "location");
    }

    public static CloudCommandSourceStack from(CommandSender sender) {
        Entity executor = sender instanceof Entity entity ? entity : null;
        Location location = executor != null ? executor.getLocation() : Location.from(sender.getServer().getDefaultLevel());
        return new CloudCommandSourceStack(sender, executor, location);
    }

    @Override
    public void sendSuccess(Component message, boolean broadcast) {
        if (broadcast) {
            CommandUtils.broadcastCommandMessage(this.sender, message);
        } else {
            this.sender.sendMessage(message);
        }
    }

    @Override
    public void sendFailure(Component message) {
        this.sender.sendMessage(message.color(NamedTextColor.RED));
    }

    @Override
    public CloudCommandSourceStack withLocation(Location location) {
        return new CloudCommandSourceStack(this.sender, this.executor, location);
    }

    @Override
    public CloudCommandSourceStack withExecutor(Entity executor) {
        return new CloudCommandSourceStack(this.sender, Objects.requireNonNull(executor, "executor"), this.location);
    }
}
