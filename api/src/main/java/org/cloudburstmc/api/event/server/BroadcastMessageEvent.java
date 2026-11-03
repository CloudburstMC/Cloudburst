package org.cloudburstmc.api.event.server;

import net.kyori.adventure.text.Component;
import org.cloudburstmc.api.command.CommandSender;
import org.cloudburstmc.api.event.Cancellable;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Called before a server broadcast is delivered. Cancelling prevents delivery.
 * Calls outside the server thread produce an asynchronous event.
 */
public class BroadcastMessageEvent extends ServerEvent implements Cancellable {

    private final Set<CommandSender> recipients;
    private Component message;

    /**
     * Creates a broadcast event with a mutable copy of the recipients.
     *
     * @param asynchronous whether the broadcast runs outside the server thread
     * @param message      the message to broadcast
     * @param recipients   the initial recipients
     * @throws NullPointerException if the message or recipient set is null
     */
    public BroadcastMessageEvent(boolean asynchronous, Component message, Set<CommandSender> recipients) {
        super(asynchronous);
        this.message = Objects.requireNonNull(message, "message");
        this.recipients = new LinkedHashSet<>(Objects.requireNonNull(recipients, "recipients"));
    }

    /**
     * Returns the component that will be sent to recipients.
     *
     * @return the broadcast message
     */
    public Component message() {
        return this.message;
    }

    /**
     * Replaces the component that will be sent to recipients.
     *
     * @param message the replacement message
     * @throws NullPointerException if the message is null
     */
    public void message(Component message) {
        this.message = Objects.requireNonNull(message, "message");
    }

    /**
     * Returns the mutable recipient set. Changes apply only to this broadcast.
     * Added recipients do not need the original broadcast permission.
     *
     * @return the recipients of this broadcast
     */
    public Set<CommandSender> getRecipients() {
        return this.recipients;
    }
}
