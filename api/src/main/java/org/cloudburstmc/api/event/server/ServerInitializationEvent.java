package org.cloudburstmc.api.event.server;

import lombok.NoArgsConstructor;

/**
 * Fired on the server thread after plugins load and before registries close.
 */
@NoArgsConstructor
public class ServerInitializationEvent extends ServerEvent {
}
