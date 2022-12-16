package org.cloudburstmc.server.event.player;

import org.cloudburstmc.api.event.Event;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.server.network.SourceInterface;

import java.net.SocketAddress;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class PlayerCreationEvent extends Event {

    private final SourceInterface interfaz;

    private final Long clientId;

    private final SocketAddress socketAddress;

    private Class<? extends Player> baseClass;

    private Class<? extends Player> playerClass;

    public PlayerCreationEvent(SourceInterface interfaz, Class<? extends Player> baseClass, Class<? extends Player> playerClass, Long clientId, SocketAddress socketAddress) {
        this.interfaz = interfaz;
        this.clientId = clientId;
        this.socketAddress = socketAddress;

        this.baseClass = baseClass;
        this.playerClass = playerClass;
    }

    public SourceInterface getInterface() {
        return interfaz;
    }

    public SocketAddress getSocketAddress() {
        return socketAddress;
    }

    public Long getClientId() {
        return clientId;
    }

    public Class<? extends Player> getBaseClass() {
        return baseClass;
    }

    public void setBaseClass(Class<? extends Player> baseClass) {
        this.baseClass = baseClass;
    }

    public Class<? extends Player> getPlayerClass() {
        return playerClass;
    }

    public void setPlayerClass(Class<? extends Player> playerClass) {
        this.playerClass = playerClass;
    }
}
