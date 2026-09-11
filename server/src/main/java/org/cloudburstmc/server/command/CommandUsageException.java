package org.cloudburstmc.server.command;

/**
 * Signals that a built-in command should show usage generated from its command tree.
 */
public final class CommandUsageException extends RuntimeException {
    public CommandUsageException() {
        super(null, null, false, false);
    }
}
