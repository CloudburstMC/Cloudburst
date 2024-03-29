package org.cloudburstmc.server.command;

import co.aikar.timings.Timing;
import co.aikar.timings.Timings;
import org.cloudburstmc.api.command.CommandSender;
import org.cloudburstmc.server.command.data.CommandData;
import org.cloudburstmc.server.command.data.CommandParameter;
import org.cloudburstmc.server.locale.TranslationContainer;
import org.cloudburstmc.server.player.CloudPlayer;
import org.cloudburstmc.server.registry.CommandRegistry;
import org.cloudburstmc.server.utils.TextFormat;

import java.util.List;

/**
 * Base class for Commands. Plugins should extend {@link PluginCommand} and not this class.
 *
 * @author MagicDroidX
 */
public abstract class Command {

    protected final CommandData commandData;

    private final String name;

    public Timing timing;

    public Command(CommandData data) {
        this(data.getRegisteredName(), data);
    }

    public Command(String name, CommandData data) {
        this.name = name.toLowerCase(); // Uppercase letters crash the client?
        this.commandData = data;
        this.timing = Timings.getCommandTiming(this);
    }

    public abstract boolean execute(CommandSender sender, String commandLabel, String[] args);

    public String getName() {
        return name;
    }

    public String getRegisteredName() {
        return this.commandData.getRegisteredName();
    }

    public void setRegisteredName(String name) {
        if (CommandRegistry.get().isClosed()) {
            throw new IllegalStateException("Trying to set registered command name outside of registration period.");
        }
        this.commandData.setRegisteredName(name);
    }

    public List<String> getPermissions() {
        return this.commandData.getPermissions();
    }

    public boolean testPermission(CommandSender target) {
        if (this.testPermissionSilent(target)) {
            return true;
        }

        if (this.commandData.getPermissionMessage().equals("")) {
            target.sendMessage(new TranslationContainer(TextFormat.RED + "%commands.generic.unknown", this.name));
        } else {
            target.sendMessage(this.commandData.getPermissionMessage());
        }

        return false;
    }

    public boolean testPermissionSilent(CommandSender target) {
        if (this.commandData.getPermissions().size() == 0) {
            return true;
        }

        for (String permission : this.commandData.getPermissions()) {
            if (target.hasPermission(permission)) {
                return true;
            }
        }

        return false;
    }

    public String getLabel() {
        return getName();
    }

    public String[] getAliases() {
        return this.commandData.getAliases().toArray(new String[0]);
    }

    public String getPermissionMessage() {
        return commandData.getPermissionMessage();
    }

    public String getDescription() {
        return this.commandData.getDescription();
    }

    public String getUsage() {
        return this.commandData.getUsage();
    }

    public List<CommandParameter[]> getCommandParameters() {
        return this.commandData.getOverloads();
    }

    public void removeAlias(String alias) {
        this.commandData.removeAlias(alias);
    }

    @Override
    public String toString() {
        return this.name;
    }

    /**
     * Generates the {@link org.cloudburstmc.protocol.bedrock.data.command.CommandData CommandData} used
     * in {@link org.cloudburstmc.protocol.bedrock.packet.AvailableCommandsPacket AvailableCommandsPacket} which
     * sends the Command data to a client. If the player does not have permission to use this Command,
     * <code>null</code> will be returned.
     *
     * @param player Player to have command packet sent
     * @return CommandData|null
     */
    public org.cloudburstmc.protocol.bedrock.data.command.CommandData toNetwork(CloudPlayer player) {
        if (!this.testPermission(player)) {
            return null;
        }
        return this.commandData.toNetwork();
    }
}
