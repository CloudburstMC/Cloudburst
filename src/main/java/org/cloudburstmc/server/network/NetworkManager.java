package org.cloudburstmc.server.network;

import io.netty.buffer.ByteBuf;
import lombok.extern.log4j.Log4j2;
import org.cloudburstmc.server.CloudServer;
import org.cloudburstmc.server.utils.Utils;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * root network management object of the server
 * everything related to networking should go here
 */
@Log4j2
public class NetworkManager {

    private final CloudServer server;

    private final NetworkStatistics statistics;

    private final Set<SourceInterface> interfaces = new HashSet<>();

    private final Set<AdvancedSourceInterface> advancedInterfaces = new HashSet<>();

    private String motd;

    private String subMotd;

    public NetworkManager(CloudServer server) {
        this.server = server;
        statistics = new NetworkStatistics();
    }

    public NetworkStatistics getStatistics() {
        return this.statistics;
    }

    public Set<SourceInterface> getInterfaces() {
        return interfaces;
    }

    public void processInterfaces() {
        for (SourceInterface interfaz : this.interfaces) {
            try {
                interfaz.process();
            } catch (Exception e) {
                interfaz.emergencyShutdown();
                this.unregisterInterface(interfaz);
                log.fatal(this.server.getLanguage().translate("cloudburst.server.networkError", interfaz.getClass().getName(), Utils.getExceptionMessage(e)));
            }
        }
    }

    public void registerInterface(SourceInterface interfaz) {
        this.interfaces.add(interfaz);
        if (interfaz instanceof AdvancedSourceInterface) {
            this.advancedInterfaces.add((AdvancedSourceInterface) interfaz);
            ((AdvancedSourceInterface) interfaz).setNetworkManager(this);
        }
        interfaz.setMotd(this.motd + "!@#" + this.subMotd);
    }

    public void unregisterInterface(SourceInterface sourceInterface) {
        this.interfaces.remove(sourceInterface);
        if (sourceInterface instanceof AdvancedSourceInterface) {
            this.advancedInterfaces.remove(sourceInterface);
        }
    }

    public String getMotd() {
        return motd;
    }

    public void setMotd(String motd) {
        this.motd = motd;
        this.updateMotds();
    }

    public String getSubMotd() {
        return subMotd;
    }

    public void setSubMotd(String subMotd) {
        this.subMotd = subMotd;
        this.updateMotds();
    }

    public void updateMotds() {
        for (SourceInterface interfaz : this.interfaces) {
            interfaz.setMotd(this.motd + "!@#" + this.subMotd);
        }
    }

    public CloudServer getServer() {
        return server;
    }

    public void sendRawPacket(InetSocketAddress socketAddress, ByteBuf payload) {
        for (AdvancedSourceInterface sourceInterface : this.advancedInterfaces) {
            sourceInterface.sendRawPacket(socketAddress, payload);
        }
    }

    public void blockAddress(InetAddress address) {
        for (AdvancedSourceInterface sourceInterface : this.advancedInterfaces) {
            sourceInterface.blockAddress(address);
        }
    }

    public void blockAddress(InetAddress address, long timeout, TimeUnit unit) {
        for (AdvancedSourceInterface sourceInterface : this.advancedInterfaces) {
            sourceInterface.blockAddress(address, timeout, unit);
        }
    }

    public void unblockAddress(InetAddress address) {
        for (AdvancedSourceInterface sourceInterface : this.advancedInterfaces) {
            sourceInterface.unblockAddress(address);
        }
    }

}
