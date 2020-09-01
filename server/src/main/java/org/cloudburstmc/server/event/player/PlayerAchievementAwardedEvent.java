package org.cloudburstmc.server.event.player;

import org.cloudburstmc.server.event.Cancellable;
import org.cloudburstmc.server.player.Player;

public class PlayerAchievementAwardedEvent extends PlayerEvent implements Cancellable {

    protected final String achievement;

    public PlayerAchievementAwardedEvent(Player player, String achievementId) {
        super(player);
        this.achievement = achievementId;
    }

    public String getAchievement() {
        return this.achievement;
    }
}
