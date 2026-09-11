package org.cloudburstmc.api.command;

import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.player.Player;

/**
 * Context for a command execution.
 */
public interface CommandSourceStack {
    /**
     * Returns the sender that initiated the command.
     *
     * @return the sender
     */
    CommandSender sender();

    /**
     * Returns the entity executing the command.
     *
     * @return the executor, or {@code null} when no entity is executing
     */
    @Nullable
    Entity executor();

    /**
     * Returns the command execution location.
     *
     * @return the execution location
     */
    Location location();

    /**
     * Returns the level where this command is executing.
     *
     * @return the execution level
     */
    default Level level() {
        return this.location().getLevel();
    }

    /**
     * Returns this source as a player.
     *
     * @return the executing player
     * @throws CommandSyntaxException if the executor is not a player
     */
    default Player playerOrThrow() throws CommandSyntaxException {
        if (this.executor() instanceof Player player) {
            return player;
        }
        throw requiresPlayer();
    }

    /**
     * Returns this source as an entity.
     *
     * @return the executing entity
     * @throws CommandSyntaxException if there is no executing entity
     */
    default Entity entityOrThrow() throws CommandSyntaxException {
        Entity executor = this.executor();
        if (executor != null) {
            return executor;
        }
        throw requiresEntity();
    }

    /**
     * Sends a success message to the sender.
     *
     * @param message   the message to send
     * @param broadcast whether administrators should be informed
     */
    void sendSuccess(Component message, boolean broadcast);

    /**
     * Sends a success message only to the sender.
     *
     * @param message the message to send
     */
    default void sendSuccess(Component message) {
        this.sendSuccess(message, false);
    }

    /**
     * Sends a success message to the sender and administrators.
     *
     * @param message the message to send
     */
    default void broadcastSuccess(Component message) {
        this.sendSuccess(message, true);
    }

    /**
     * Sends a failure message to the sender.
     *
     * @param message the message to send
     */
    void sendFailure(Component message);

    /**
     * Returns a copy of this source at a different location while preserving its sender and executor.
     *
     * @param location the new location
     * @return the copied source
     */
    CommandSourceStack withLocation(Location location);

    /**
     * Returns a copy of this source with a different executing entity while preserving its sender and location.
     *
     * @param executor the new executor
     * @return the copied source
     */
    CommandSourceStack withExecutor(Entity executor);

    private static CommandSyntaxException requiresPlayer() {
        return new SimpleCommandExceptionType(new LiteralMessage("This command requires a player")).create();
    }

    private static CommandSyntaxException requiresEntity() {
        return new SimpleCommandExceptionType(new LiteralMessage("This command requires an entity")).create();
    }
}
