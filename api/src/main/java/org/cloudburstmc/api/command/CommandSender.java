package org.cloudburstmc.api.command;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.cloudburstmc.api.Server;
import org.cloudburstmc.api.permission.Permissible;

/**
 * Represents the sender of a command.
 */
public interface CommandSender extends Audience, Permissible {

    /**
     * Sends a component message to this sender.
     *
     * <p>Implementations decide how the component is represented for their target, such as an in-game
     * message or plain rendered text for the console.</p>
     *
     * @param message the message to send
     */
    @Override
    void sendMessage(@NonNull Component message);

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
}
