package org.cloudburstmc.api.event.server;

import lombok.Getter;
import org.cloudburstmc.api.command.CommandSender;
import org.cloudburstmc.api.event.Cancellable;

import java.util.Objects;

/**
 * Fired before a command submitted by a non-player sender is dispatched.
 *
 * <p>Cancelling this event prevents execution. Command lines may include or omit a leading slash.</p>
 */
public final class ServerCommandEvent extends ServerEvent implements Cancellable {

    /**
     * Sender that submitted the command.
     */
    @Getter
    private final CommandSender sender;
    /**
     * Command line that will be dispatched.
     */
    @Getter
    private String command;

    /**
     * Creates a server command event.
     *
     * @param sender  the command sender
     * @param command the command line
     */
    public ServerCommandEvent(CommandSender sender, String command) {
        this.sender = Objects.requireNonNull(sender, "sender");
        this.setCommand(command);
    }

    /**
     * Replaces the command line that will be dispatched.
     *
     * @param command the replacement command line
     * @throws NullPointerException     if the command is {@code null}
     * @throws IllegalArgumentException if the command is blank
     */
    public void setCommand(String command) {
        Objects.requireNonNull(command, "command");
        if (command.isBlank()) {
            throw new IllegalArgumentException("Server command must not be blank");
        }
        this.command = command;
    }
}
