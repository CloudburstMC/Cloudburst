package org.cloudburstmc.server.network;

import net.kyori.adventure.text.Component;
import org.cloudburstmc.api.player.GameMode;

public record CloudServerStatus(Component motd, String levelName, GameMode gameMode, int playerCount, int maxPlayerCount, boolean hardcore, boolean onlineAuth) {
}
