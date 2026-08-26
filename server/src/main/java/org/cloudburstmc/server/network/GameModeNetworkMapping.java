package org.cloudburstmc.server.network;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.player.GameMode;
import org.cloudburstmc.protocol.bedrock.data.GameType;

@UtilityClass
public class GameModeNetworkMapping {

    public static GameType forStartGame(GameMode gameMode) {
        return gameMode == GameMode.SPECTATOR ? GameType.SURVIVAL : forPlayer(gameMode);
    }

    public static GameType forPlayer(GameMode gameMode) {
        if (gameMode == GameMode.SPECTATOR) {
            return GameType.SPECTATOR;
        }

        return GameType.from(gameMode.getVanillaId());
    }

    public static int playerTypeId(GameMode gameMode) {
        return forPlayer(gameMode).ordinal();
    }
}
