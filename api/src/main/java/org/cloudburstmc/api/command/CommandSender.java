package org.cloudburstmc.api.command;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.cloudburstmc.api.Server;
import org.cloudburstmc.api.permission.Permissible;

/**
 * Represents the sender of a command.
 */
public interface CommandSender extends Audience, Permissible {

    /**
     * Returns the server instance associated with this command sender.
     *
     * @return the server instance
     */
    Server getServer();

    /**
     * Returns the name of this command sender.
     *
     * @return the name
     */
    String getName();

    /**
     * Returns the display name of this command sender.
     *
     * @return the display name component
     */
    Component name();

    /**
     * Returns whether this command sender represents an online player.
     *
     * @return true if this sender is a player
     */
    boolean isPlayer();
}
