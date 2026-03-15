package org.cloudburstmc.server.command;

import com.google.inject.Singleton;
import lombok.extern.log4j.Log4j2;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.translation.GlobalTranslator;
import org.cloudburstmc.api.command.CommandSender;
import org.cloudburstmc.api.permission.Permission;
import org.cloudburstmc.api.permission.PermissionAttachment;
import org.cloudburstmc.api.permission.PermissionAttachmentInfo;
import org.cloudburstmc.api.plugin.PluginContainer;
import org.cloudburstmc.server.CloudServer;
import org.cloudburstmc.server.permission.PermissibleBase;

import java.util.Map;

/**
 * Represents the server console as a command sender.
 */
@Log4j2
@Singleton
public class ConsoleCommandSender implements CommandSender {

    private final PermissibleBase perm;

    public ConsoleCommandSender() {
        this.perm = new PermissibleBase(this);
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
    public PermissionAttachment addAttachment(PluginContainer plugin, String name, Boolean value) {
        return this.perm.addAttachment(plugin, name, value);
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
    public Map<String, PermissionAttachmentInfo> getEffectivePermissions() {
        return this.perm.getEffectivePermissions();
    }

    public boolean isPlayer() {
        return false;
    }

    @Override
    public CloudServer getServer() {
        return CloudServer.getInstance();
    }

    @Override
    public void sendMessage(Component message) {
        Component rendered = GlobalTranslator.render(message, CloudServer.getInstance().getLanguage().getLocale());
        String text = LegacyComponentSerializer.legacySection().serialize(rendered);
        for (String line : text.trim().split("\n")) {
            log.info(line);
        }
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
    }
}
