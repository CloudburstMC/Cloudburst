package org.cloudburstmc.server.network;

import io.netty.buffer.ByteBuf;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public interface AdvancedSourceInterface extends SourceInterface {

    void blockAddress(InetAddress address);

    void blockAddress(InetAddress address, long timeout, TimeUnit unit);

    void unblockAddress(InetAddress address);

    void setNetworkManager(NetworkManager networkManager);

    void sendRawPacket(InetSocketAddress socketAddress, ByteBuf payload);
}
