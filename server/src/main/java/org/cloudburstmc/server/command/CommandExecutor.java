package org.cloudburstmc.server.command;

import org.cloudburstmc.api.command.CommandSender;

/**
 * Represents a class which contains a single method for command execution.
 */
public interface CommandExecutor {

    /**
     * Called when a command is executed.
     *
     * If this method returns false, Cloudburst will send the command usage
     * message for this command to the sender. If the command execution was
     * successful, you should return true.
     *
     * If you want to test whether a command sender has the permission to execute
     * a command, you can use {@link Command#testPermissionSilent}.
     *
     * @param sender The object that executed the command
     * @param command The command which was executed
     * @param label The alias used to execute the command
     * @param args Passed command arguments
     *
     * @return true if the command executed successfully
     */
    boolean onCommand(CommandSender sender, Command command, String label, String[] args);
}
