package org.cloudburstmc.server.command;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.log4j.Log4j2;
import net.kyori.adventure.chat.ChatType;
import net.kyori.adventure.chat.SignedMessage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.translation.GlobalTranslator;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.command.CommandSender;
import org.cloudburstmc.api.permission.Permission;
import org.cloudburstmc.api.permission.PermissionAttachment;
import org.cloudburstmc.api.permission.PermissionAttachmentInfo;
import org.cloudburstmc.api.permission.PermissionManager;
import org.cloudburstmc.api.plugin.PluginContainer;
import org.cloudburstmc.server.CloudServer;
import org.cloudburstmc.server.permission.PermissibleBase;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Set;

/**
 * Represents the server console as a command sender.
 *
 * <p>The console is always an operator and its op status cannot be changed.</p>
 */
@Log4j2
@Singleton
public class ConsoleCommandSender implements CommandSender {

    private final PermissibleBase perm;

    @Inject
    public ConsoleCommandSender(PermissionManager permissionManager) {
        this.perm = new PermissibleBase(permissionManager, this);
    }

    @Override
    public boolean isPermissionSet(String name) {
        return this.perm.isPermissionSet(name);
    }

    @Override
    public boolean isPermissionSet(Permission permission) {
        return this.perm.isPermissionSet(permission);
    }

    @Override
    public boolean hasPermission(String name) {
        return this.perm.hasPermission(name);
    }

    @Override
    public boolean hasPermission(Permission permission) {
        return this.perm.hasPermission(permission);
    }

    @Override
    public PermissionAttachment addAttachment(PluginContainer plugin) {
        return this.perm.addAttachment(plugin);
    }

    @Override
    public PermissionAttachment addAttachment(PluginContainer plugin, String name) {
        return this.perm.addAttachment(plugin, name);
    }

    @Override
    public PermissionAttachment addAttachment(PluginContainer plugin, String name, boolean value) {
        return this.perm.addAttachment(plugin, name, value);
    }

    @Override
    public @Nullable PermissionAttachment addAttachment(PluginContainer plugin, long ticks) {
        return this.perm.addAttachment(plugin, ticks);
    }

    @Override
    public @Nullable PermissionAttachment addAttachment(PluginContainer plugin, String name, boolean value, long ticks) {
        return this.perm.addAttachment(plugin, name, value, ticks);
    }

    @Override
    public void removeAttachment(PermissionAttachment attachment) {
        this.perm.removeAttachment(attachment);
    }

    @Override
    public void recalculatePermissions() {
        this.perm.recalculatePermissions();
    }

    @Override
    public Set<PermissionAttachmentInfo> getEffectivePermissions() {
        return this.perm.getEffectivePermissions();
    }

    @Override
    public boolean isPlayer() {
        return false;
    }

    @Override
    public CloudServer getServer() {
        return CloudServer.getInstance();
    }

    @Override
    public void sendMessage(@NotNull Component message) {
        Component rendered = GlobalTranslator.render(message, CloudServer.getInstance().getLanguage().getLocale());
        String text = PlainTextComponentSerializer.plainText().serialize(rendered);
        for (String line : text.split("\\R")) {
            log.info(line);
        }
    }

    @Override
    public void sendMessage(@NotNull Component message, ChatType.@NotNull Bound boundChatType) {
        this.sendMessage(message);
    }

    @Override
    public void sendMessage(@NotNull SignedMessage signedMessage, ChatType.@NotNull Bound boundChatType) {
        Component message = Objects.requireNonNullElseGet(signedMessage.unsignedContent(),
                () -> Component.text(signedMessage.message()));
        this.sendMessage(message, boundChatType);
    }

    @Override
    public String getName() {
        return "CONSOLE";
    }

    @Override
    public Component name() {
        return Component.text("CONSOLE");
    }

    @Override
    public boolean isOp() {
        return true;
    }

    @Override
    public void setOp(boolean value) {
        throw new UnsupportedOperationException("Cannot change op status of the console");
    }
}
