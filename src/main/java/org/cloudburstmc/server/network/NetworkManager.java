package org.cloudburstmc.server.network;

import io.netty.buffer.ByteBuf;
import lombok.extern.log4j.Log4j2;
import org.cloudburstmc.server.CloudServer;
import org.cloudburstmc.server.network.query.QueryHandler;
import org.cloudburstmc.server.permission.BanEntry;
import org.cloudburstmc.server.utils.Utils;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
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

    private final QueryHandler queryHandler;

    private String motd;

    private String subMotd;

    public NetworkManager(CloudServer server) {
        this.server = server;
        statistics = new NetworkStatistics();
        if (this.server.getConfig().isEnableQuery()) {
            this.queryHandler = new QueryHandler();
        } else {
            this.queryHandler = null;
        }
    }


    public void start() throws Exception {
        registerInterface(new BedrockInterface(this.server));
    }

    public void close() {
        interfaces.forEach(SourceInterface::shutdown);
        interfaces.clear();
    }


    public CloudServer getServer() {
        return server;
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
        interfaz.setNetworkManager(this);
        interfaz.setMotd(this.motd, this.subMotd);
    }

    public void unregisterInterface(SourceInterface sourceInterface) {
        this.interfaces.remove(sourceInterface);
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
        this.interfaces.forEach(it -> it.setMotd(this.motd, this.subMotd));
    }


    public void sendRawPacket(InetSocketAddress socketAddress, ByteBuf payload) {
        this.interfaces.forEach(it -> it.sendRawPacket(socketAddress, payload));
    }


    public void blockAddress(InetAddress address) {
        this.interfaces.forEach(it -> it.blockAddress(address));
    }

    public void blockAddress(InetAddress address, long timeout, TimeUnit unit) {
        this.interfaces.forEach(it -> it.blockAddress(address, timeout, unit));
    }

    public void unblockAddress(InetAddress address) {
        this.interfaces.forEach(it -> it.unblockAddress(address));
    }

    public QueryHandler getQueryHandler() {
        return queryHandler;
    }
}
