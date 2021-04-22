package org.cloudburstmc.server.network;


import io.netty.buffer.ByteBuf;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

/**
 * inbound network interface, can handle ping request, blocking by address and send packets
 */
public interface SourceInterface {

    void setMotd(String motd, String subMotd);

    boolean process();

    void shutdown();

    void emergencyShutdown();

    void blockAddress(InetAddress address);

    void blockAddress(InetAddress address, long timeout, TimeUnit unit);

    void unblockAddress(InetAddress address);

    void setNetworkManager(NetworkManager networkManager);

    void sendRawPacket(InetSocketAddress socketAddress, ByteBuf payload);
}
