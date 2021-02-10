package org.cloudburstmc.api.event.player;

import org.cloudburstmc.api.player.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * This event is called asynchronously
 *
 * @author CreeperFace
 */
public final class PlayerAsyncPreLoginEvent extends PlayerEvent {

//    private final PlayerLoginData loginData;

    private LoginResult loginResult = LoginResult.SUCCESS;
    private String kickMessage = "Plugin Reason";

    private final List<Consumer<Player>> scheduledActions = new ArrayList<>();

    public PlayerAsyncPreLoginEvent(Player player) {
        super(player);
    }

/*    public PlayerAsyncPreLoginEvent(PlayerLoginData loginData) {
        super(null);
        this.loginData = loginData;
    }

    public PlayerLoginData getLoginData() {
        return loginData;
    }*/

    public LoginResult getLoginResult() {
        return loginResult;
    }

    public void setLoginResult(LoginResult loginResult) {
        this.loginResult = loginResult;
    }

    public String getKickMessage() {
        return kickMessage;
    }

    public void setKickMessage(String kickMessage) {
        this.kickMessage = kickMessage;
    }

    public void scheduleSyncAction(Consumer<Player> action) {
        this.scheduledActions.add(action);
    }

    public List<Consumer<Player>> getScheduledActions() {
        return new ArrayList<>(scheduledActions);
    }

    public void allow() {
        this.loginResult = LoginResult.SUCCESS;
    }

    public void disAllow(String message) {
        this.loginResult = LoginResult.KICK;
        this.kickMessage = message;
    }

    @Override
    public Player getPlayer() {
        throw new UnsupportedOperationException("No Player instance provided in async event");
    }

    public enum LoginResult {
        SUCCESS,
        KICK
    }
}
