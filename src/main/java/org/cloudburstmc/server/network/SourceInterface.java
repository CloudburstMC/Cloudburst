package org.cloudburstmc.server.network;


import io.netty.buffer.ByteBuf;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

/**
 * inbound network interface, can handle ping request, blocking by address and send packets
 */
public interface SourceInterface {

    /**
     * set the main motd and submotd returned on client ping
     */
    void setMotd(String motd, String subMotd);

    /**
     * perform some process need to be done per tick, used on ticking
     */
    boolean process();

    /**
     * gracefully shutdown the interface
     */
    void shutdown();

    /**
     * shutdown immediately
     */
    void emergencyShutdown();

    /**
     * block an address indefinitely
     */
    void blockAddress(InetAddress address);

    /**
     * block an address for specified amount of time
     */
    void blockAddress(InetAddress address, long timeout, TimeUnit unit);

    /**
     * unblock an address
     */
    void unblockAddress(InetAddress address);

    /**
     * send raw UDP packet to target address
     */
    void sendRawPacket(InetSocketAddress socketAddress, ByteBuf payload);

    /**
     * set the manager that this interface belongs to
     */
    void setNetworkManager(NetworkManager networkManager);

}
