package org.cloudburstmc.server.player;

import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.Level;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.protocol.bedrock.BedrockServerSession;
import org.cloudburstmc.server.CloudServer;
import org.cloudburstmc.server.event.player.PlayerCreationEvent;
import org.cloudburstmc.server.network.BedrockInterface;
import org.cloudburstmc.server.utils.ClientChainData;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.function.Consumer;

@Log4j2
public class PlayerLoginData {
    private final BedrockServerSession session;
    private final CloudServer server;
    private final BedrockInterface interfaz;

    private Runnable preLoginEventTask;
    private String username;
    private ClientChainData chainData;
    private boolean shouldLogin;
    private volatile boolean preLoginDone;
    private List<Consumer<Player>> loginTasks;
    private boolean clientCacheEnabled;

    public PlayerLoginData(BedrockServerSession session, CloudServer server, BedrockInterface interfaz) {
        this.session = session;
        this.server = server;
        this.interfaz = interfaz;
        shouldLogin = false;
    }

    public CloudPlayer initializePlayer() {
        CloudPlayer player;

        PlayerCreationEvent ev = new PlayerCreationEvent(interfaz, CloudPlayer.class, CloudPlayer.class, this.chainData.getClientId(), session.getSocketAddress());
        this.server.getEventManager().fire(ev);
        Class<? extends CloudPlayer> clazz = (Class<? extends CloudPlayer>) ev.getPlayerClass();

        try {
            Constructor<? extends CloudPlayer> constructor = clazz.getConstructor(BedrockServerSession.class, ClientChainData.class);
            player = constructor.newInstance(session, chainData);
            this.server.addPlayer(session.getSocketAddress(), player);
        } catch (NoSuchMethodException | InvocationTargetException | InstantiationException |
                 IllegalAccessException e) {
            log.throwing(Level.ERROR, e);
            return null;
        }

        player.processLogin();
        player.setClientCacheEnabled(this.clientCacheEnabled);
        player.completeLoginSequence();

        return player;
    }

    public Runnable getPreLoginEventTask() {
        return preLoginEventTask;
    }

    public void setPreLoginEventTask(Runnable preLoginEventTask) {
        this.preLoginEventTask = preLoginEventTask;
    }

    public ClientChainData getChainData() {
        return chainData;
    }

    public void setChainData(ClientChainData chainData) {
        this.chainData = chainData;
    }

    public boolean isShouldLogin() {
        return shouldLogin;
    }

    public void setShouldLogin(boolean shouldLogin) {
        this.shouldLogin = shouldLogin;
    }

    public boolean isPreLoginDone() {
        return preLoginDone;
    }

    public void setPreLoginDone(boolean preLoginDone) {
        this.preLoginDone = preLoginDone;
    }

    public BedrockServerSession getSession() {
        return session;
    }

    public String getName() {
        return username;
    }

    public void setName(String username) {
        this.username = username;
    }

    public List<Consumer<Player>> getLoginTasks() {
        return loginTasks;
    }

    public void setLoginTasks(List<Consumer<Player>> tasks) {
        this.loginTasks = tasks;
    }

    public boolean isClientCacheEnabled() {
        return clientCacheEnabled;
    }

    public void setClientCacheEnabled(boolean clientCacheEnabled) {
        this.clientCacheEnabled = clientCacheEnabled;
    }
}
