package org.cloudburstmc.server.network.inventory.filter;

import org.cloudburstmc.api.player.Player;

import java.util.List;

public class NoopProcessor implements TextFilteringProcessor {
    @Override
    public Runnable processCommandsAndNames(Player player, List<String> messages) {
        return () -> {
        };
    }

    @Override
    public Runnable processMessages(Player player, List<String> messages) {
        return () -> {
        };
    }
}
