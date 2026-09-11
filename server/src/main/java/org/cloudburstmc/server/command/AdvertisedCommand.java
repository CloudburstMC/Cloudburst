package org.cloudburstmc.server.command;

import org.cloudburstmc.server.command.network.CommandNetworkData;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Base for built-in commands advertised to clients.
 */
public abstract class AdvertisedCommand extends ServerCommand {
    private final CommandNetworkData networkData;

    protected AdvertisedCommand(
            String name,
            String description,
            Collection<String> aliases,
            Collection<String> accessPermissions,
            CommandNetworkData networkData
    ) {
        super(name, description, aliases, accessPermissions);
        this.networkData = Objects.requireNonNull(networkData, "networkData");
    }

    protected AdvertisedCommand(
            String name,
            String description,
            Collection<String> aliases,
            Collection<String> accessPermissions
    ) {
        this(name, description, aliases, accessPermissions, CommandNetworkData.ANY_NOT_CHEAT);
    }

    protected AdvertisedCommand(String name, String description, Collection<String> aliases, CommandNetworkData networkData, String... permissions) {
        this(name, description, aliases, Arrays.asList(permissions), networkData);
    }

    protected AdvertisedCommand(String name, String description, Collection<String> aliases, String... permissions) {
        this(name, description, aliases, Arrays.asList(permissions));
    }

    protected AdvertisedCommand(String name, String description, CommandNetworkData networkData, String... permissions) {
        this(name, description, List.of(), Arrays.asList(permissions), networkData);
    }

    protected AdvertisedCommand(String name, String description, String... permissions) {
        this(name, description, List.of(), Arrays.asList(permissions));
    }

    /**
     * Returns the metadata used to advertise this command to clients.
     *
     * @return client command metadata
     */
    public final CommandNetworkData networkData() {
        return this.networkData;
    }
}
