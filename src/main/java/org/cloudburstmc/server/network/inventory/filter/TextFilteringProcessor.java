package org.cloudburstmc.server.network.inventory.filter;

import org.cloudburstmc.api.player.Player;

import java.util.List;

public interface TextFilteringProcessor {

    Runnable processCommandsAndNames(Player player, List<String> messages);

    Runnable processMessages(Player player, List<String> messages);
}
