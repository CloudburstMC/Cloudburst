package org.cloudburstmc.api.event.server;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.cloudburstmc.api.command.Commands;

/**
 * Fired while command registration is open.
 *
 * <p>Plugins should register Brigadier command trees through {@link #getCommands()} during this event.</p>
 */
@RequiredArgsConstructor
public class CommandRegistrationEvent extends ServerEvent {

    /**
     * Command registrar for this registration phase.
     */
    @NonNull
    private final Commands commands;

    /**
     * Returns the registrar available during this registration phase.
     *
     * @return the command registrar
     */
    @NonNull
    public Commands getCommands() {
        return this.commands;
    }
}
