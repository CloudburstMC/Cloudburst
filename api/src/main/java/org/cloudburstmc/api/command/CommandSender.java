package org.cloudburstmc.api.command;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
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

    default void sendMessage(@NonNull String message) {
        this.sendMessage(Component.text(message));
    }

    /**
     * Sends literal text without interpreting formatting tags.
     *
     * @param message the plain text to send
     */
    default void sendPlainMessage(@NonNull String message) {
        this.sendMessage(Component.text(message));
    }

    /**
     * Parses MiniMessage text and sends the resulting component.
     *
     * @param message the MiniMessage text to send
     */
    default void sendRichMessage(@NonNull String message) {
        this.sendMessage(MiniMessage.miniMessage().deserialize(message, this));
    }

    /**
     * Parses MiniMessage text with additional tag resolvers and sends the resulting component.
     * Resolvers receive this sender as their target and can override standard tags.
     *
     * <p>Use {@link net.kyori.adventure.text.minimessage.tag.resolver.Placeholder#unparsed(String, String)}
     * for replacement text that must not be interpreted as formatting.</p>
     *
     * @param message   the MiniMessage text to send
     * @param resolvers the additional tag resolvers
     */
    default void sendRichMessage(@NonNull String message, @NonNull TagResolver... resolvers) {
        this.sendMessage(MiniMessage.miniMessage().deserialize(message, this, resolvers));
    }

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
