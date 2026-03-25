package org.cloudburstmc.api.event.player;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.cloudburstmc.api.event.Cancellable;
import org.cloudburstmc.api.player.Player;

import java.util.Objects;
import java.util.Set;

/**
 * Fired when a player sends a chat message. The event exposes the original unmodified message
 * as well as a mutable rendered component and a mutable set of recipient audiences.
 *
 * <p>A {@link ChatRenderer} controls how the final rendered component is built from the player's
 * name and message. Replace the renderer with a custom implementation to change the
 * format of chat messages.</p>
 */
public final class PlayerChatEvent extends PlayerEvent implements Cancellable {

    private final Component originalMessage;
    private final Set<Audience> viewers;
    private Component message;
    private ChatRenderer renderer;

    /**
     * Creates a new chat event.
     *
     * @param player          the player sending the message
     * @param originalMessage the raw message text exactly as the client sent it
     * @param renderer        the renderer used to build the chat line sent to each viewer
     * @param viewers         the mutable set of audiences that will receive the rendered message
     */
    public PlayerChatEvent(Player player, Component originalMessage, ChatRenderer renderer, Set<Audience> viewers) {
        super(player);
        this.originalMessage = Objects.requireNonNull(originalMessage, "originalMessage");
        this.message = originalMessage;
        this.renderer = Objects.requireNonNull(renderer, "renderer");
        this.viewers = Objects.requireNonNull(viewers, "viewers");
    }

    /**
     * Returns the original unmodified message as received from the client. This component is
     * immutable and is never changed by the event system.
     *
     * @return the original message
     */
    public Component originalMessage() {
        return originalMessage;
    }

    /**
     * Returns the current message component. Call {@link #message(Component)} to
     * replace this before the message is passed to the renderer.
     *
     * @return the current message component
     */
    public Component message() {
        return message;
    }

    /**
     * Replaces the message component that will be passed to the renderer.
     *
     * @param message the new message component
     */
    public void message(Component message) {
        this.message = Objects.requireNonNull(message, "message");
    }

    /**
     * Returns the renderer that builds the chat line for each viewer.
     *
     * @return the current renderer
     */
    public ChatRenderer renderer() {
        return renderer;
    }

    /**
     * Replaces the renderer.
     *
     * @param renderer the new renderer
     */
    public void renderer(ChatRenderer renderer) {
        this.renderer = Objects.requireNonNull(renderer, "renderer");
    }

    /**
     * Returns the mutable set of audiences that will receive the rendered chat message.
     * Add or remove entries to control who sees the message.
     *
     * @return the mutable viewer set
     */
    public Set<Audience> viewers() {
        return viewers;
    }
}
