package org.cloudburstmc.api.event.player;

import org.cloudburstmc.api.event.Cancellable;
import org.cloudburstmc.api.player.Player;

public final class PlayerAchievementAwardedEvent extends PlayerEvent implements Cancellable {

    protected final String achievement;

    public PlayerAchievementAwardedEvent(Player player, String achievementId) {
        super(player);
        this.achievement = achievementId;
    }

    public String getAchievement() {
        return this.achievement;
    }
}
