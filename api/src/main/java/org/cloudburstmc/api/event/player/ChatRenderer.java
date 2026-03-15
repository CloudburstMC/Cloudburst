package org.cloudburstmc.api.event.player;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.cloudburstmc.api.player.Player;

/**
 * Renders a chat message for a specific audience member.
 *
 * <p>A renderer is called once per viewer when a {@link PlayerChatEvent} is dispatched; it
 * receives the sending player, the player's display name, the (possibly modified) message
 * component, and the target audience, and must return the final component shown to that
 * viewer.</p>
 *
 * <p>Use {@link #defaultRenderer()} for the standard {@code "chat.type.text"} format, or
 * {@link #viewerUnaware(ViewerUnawareRenderer)} to create a renderer whose output is the same
 * for every viewer.</p>
 */
@FunctionalInterface
public interface ChatRenderer {

    /**
     * Returns a renderer that formats messages using the vanilla {@code chat.type.text}
     * translatable key: {@code <sourceName>: <message>}.
     *
     * @return the default chat renderer
     */
    static ChatRenderer defaultRenderer() {
        return (source, sourceName, message, viewer) -> Component.translatable("chat.type.text", sourceName, message);
    }

    /**
     * Wraps a viewer-unaware renderer so that the same component is returned for every viewer.
     *
     * @param renderer a renderer that does not need per-viewer customization
     * @return a {@link ChatRenderer} backed by the given viewer-unaware renderer
     */
    static ChatRenderer viewerUnaware(ViewerUnawareRenderer renderer) {
        return (source, sourceName, message, viewer) -> renderer.render(source, sourceName, message);
    }

    /**
     * Renders the chat message for the given audience.
     *
     * @param source      the player who sent the message
     * @param sourceName  the display name of the source player at the time of sending
     * @param message     the message component (may have been modified by event handlers)
     * @param viewer      the audience receiving the message
     * @return the rendered component to deliver to {@code viewer}
     */
    Component render(Player source, Component sourceName, Component message, Audience viewer);

    /**
     * A simplified renderer variant whose output does not vary per viewer.
     */
    @FunctionalInterface
    interface ViewerUnawareRenderer {

        /**
         * Renders the chat message.
         *
         * @param source     the player who sent the message
         * @param sourceName the display name of the source player
         * @param message    the message component
         * @return the rendered component
         */
        Component render(Player source, Component sourceName, Component message);
    }
}
