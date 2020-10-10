package org.cloudburstmc.server.command;

import lombok.extern.log4j.Log4j2;
import org.cloudburstmc.api.plugin.PluginContainer;
import org.cloudburstmc.server.CloudServer;
import org.cloudburstmc.server.locale.TextContainer;
import org.cloudburstmc.server.locale.TranslationContainer;
import org.cloudburstmc.server.permission.PermissibleBase;
import org.cloudburstmc.server.permission.Permission;
import org.cloudburstmc.server.permission.PermissionAttachment;
import org.cloudburstmc.server.permission.PermissionAttachmentInfo;

import javax.inject.Singleton;
import java.util.Map;

/**
 * author: MagicDroidX
 * Nukkit Project
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
    public void sendMessage(String message) {
        message = this.getServer().getLanguage().translate(message);
        for (String line : message.trim().split("\n")) {
            log.info(line);
        }
    }

    @Override
    public void sendMessage(TextContainer message) {
        Object[] args = null;
        if (message instanceof TranslationContainer) {
            args = ((TranslationContainer) message).getParameters();
        }
        this.sendMessage(this.getServer().getLanguage().translate(message.getText(), args));
    }

    @Override
    public String getName() {
        return "CONSOLE";
    }

    @Override
    public boolean isOp() {
        return true;
    }

    @Override
    public void setOp(boolean value) {

    }
}
