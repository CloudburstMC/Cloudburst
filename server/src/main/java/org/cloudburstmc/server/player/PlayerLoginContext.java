package org.cloudburstmc.server.player;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import net.kyori.adventure.text.Component;
import org.apache.logging.log4j.Level;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.protocol.adventure.BedrockComponent;
import org.cloudburstmc.protocol.bedrock.BedrockServerSession;
import org.cloudburstmc.protocol.bedrock.data.DisconnectFailReason;
import org.cloudburstmc.protocol.bedrock.packet.DisconnectPacket;
import org.cloudburstmc.server.CloudServer;
import org.cloudburstmc.server.event.player.PlayerCreationEvent;
import org.cloudburstmc.server.network.BedrockInterface;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.function.Consumer;

@Log4j2
public class PlayerLoginContext {
    @Getter
    private final BedrockServerSession session;
    private final CloudServer server;
    private final BedrockInterface interfaz;

    @Getter
    @Setter
    private AuthenticatedPlayerData playerData;
    @Getter
    @Setter
    private boolean clientCacheEnabled;

    private boolean preLoginComplete;
    private boolean resourcePacksComplete;
    private boolean playerInitialized;
    private List<Consumer<Player>> loginTasks = List.of();

    public PlayerLoginContext(BedrockServerSession session, CloudServer server, BedrockInterface interfaz) {
        this.session = session;
        this.server = server;
        this.interfaz = interfaz;
    }

    public synchronized void completePreLogin(List<Consumer<Player>> loginTasks) {
        this.loginTasks = List.copyOf(loginTasks);
        this.preLoginComplete = true;
        this.initializePlayerIfReady();
    }

    public synchronized void completeResourcePacks() {
        this.resourcePacksComplete = true;
        this.initializePlayerIfReady();
    }

    public void disconnect(DisconnectFailReason reason, Component message) {
        DisconnectPacket packet = new DisconnectPacket();
        packet.setReason(reason);
        BedrockComponent bedrockMessage = new BedrockComponent(message);
        packet.setKickMessage(bedrockMessage);
        this.session.sendPacketImmediately(packet);
        this.session.disconnect(bedrockMessage);
    }

    private void initializePlayerIfReady() {
        if (!this.preLoginComplete || !this.resourcePacksComplete || this.playerInitialized) {
            return;
        }

        this.playerInitialized = true;
        CloudPlayer player = this.createPlayer();
        if (player == null) {
            return;
        }

        for (Consumer<Player> loginTask : this.loginTasks) {
            loginTask.accept(player);
        }
    }

    private CloudPlayer createPlayer() {
        CloudPlayer player;
        PlayerCreationEvent ev = new PlayerCreationEvent(this.interfaz, CloudPlayer.class, CloudPlayer.class, this.playerData.getClientId(), this.session.getSocketAddress());
        this.server.getEventManager().fire(ev);
        Class<? extends CloudPlayer> clazz = ev.getPlayerClass().asSubclass(CloudPlayer.class);

        try {
            Constructor<? extends CloudPlayer> constructor = clazz.getConstructor(BedrockServerSession.class, AuthenticatedPlayerData.class);
            player = constructor.newInstance(this.session, this.playerData);
            this.server.addPlayer(this.session.getSocketAddress(), player);
        } catch (NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
            log.throwing(Level.ERROR, e);
            return null;
        }

        player.processLogin();
        player.setClientCacheEnabled(this.clientCacheEnabled);
        player.completeLoginSequence();

        return player;
    }
}
