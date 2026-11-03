package org.cloudburstmc.api.event.server;

import lombok.NoArgsConstructor;

/**
 * Fired on the server thread during shutdown before plugins and worlds are closed.
 */
@NoArgsConstructor
public class ServerShutdownEvent extends ServerEvent {
}
