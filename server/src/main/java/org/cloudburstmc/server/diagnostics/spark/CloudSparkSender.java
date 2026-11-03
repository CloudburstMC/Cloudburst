package org.cloudburstmc.server.diagnostics.spark;

import me.lucko.spark.common.command.sender.AbstractCommandSender;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.command.CommandSender;
import org.cloudburstmc.api.player.Player;

import java.util.UUID;
import java.util.concurrent.RejectedExecutionException;

/**
 * Adapts command identity and permissions while delivering profiler messages on the server thread.
 */
public class CloudSparkSender extends AbstractCommandSender<CommandSender> {

    private final CloudSpark spark;

    public CloudSparkSender(CloudSpark spark, CommandSender sender) {
        super(sender);
        this.spark = spark;
    }

    @Override
    public String getName() {
        return this.delegate.getName();
    }

    @Override
    public @Nullable UUID getUniqueId() {
        return this.delegate instanceof Player player ? player.getProfile().getUniqueId() : null;
    }

    @Override
    public void sendMessage(Component message) {
        try {
            this.spark.executeSync(() -> this.delegate.sendMessage(message));
        } catch (RejectedExecutionException ignored) {
            // Profiler responses are discarded once the server starts shutting down.
        }
    }

    @Override
    public boolean hasPermission(String permission) {
        return this.delegate.hasPermission(permission);
    }
}
