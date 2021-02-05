package org.cloudburstmc.api.command;

import org.cloudburstmc.api.Server;
import org.cloudburstmc.api.locale.TextContainer;
import org.cloudburstmc.api.permission.Permissible;

/**
 * Represents the sender of a command.
 */
public interface CommandSender extends Permissible {

    /**
     * Sends a message to the command sender.
     *
     * @param message The message to send
     */
    void sendMessage(String message);

    /**
     * Sends a message to the command sender.
     *
     * @param message The message to send.
     */
    void sendMessage(TextContainer message);

    /**
     * Returns the server instance of the command sender.
     *
     * @return The server instance
     */
    Server getServer();

    /**
     * Returns the name of the command sender.
     *
     * @return The name of the command sender.
     */
    String getName();

    /**
     * Checks if this command sender is a player.
     *
     * @return true if the sender is a player
     */
    boolean isPlayer();
}
