package cn.nukkit.raknet.protocol.packet;

import cn.nukkit.raknet.protocol.Packet;

import java.net.InetSocketAddress;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class CLIENT_HANDSHAKE_DataPacket extends Packet {
    public static byte ID = (byte) 0x13;

    @Override
    public byte getID() {
        return ID;
    }

    public String address;
    public int port;
    public InetSocketAddress[] systemAddresses = new InetSocketAddress[10];

    public long sendPing;
    public long sendPong;

    @Override
    public void encode() {
    }

    @Override
    public void decode() {
        super.decode();
        InetSocketAddress addr = this.getAddress();
        this.address = addr.getHostString();
        this.port = addr.getPort();

        for (int i = 0; i < 10; i++) {
            this.systemAddresses[i] = this.getAddress();
        }

        this.sendPing = this.getLong();
        this.sendPong = this.getLong();
    }

    @Override
    public CLIENT_HANDSHAKE_DataPacket clone() throws CloneNotSupportedException {
        CLIENT_HANDSHAKE_DataPacket packet = (CLIENT_HANDSHAKE_DataPacket) super.clone();
        packet.systemAddresses = this.systemAddresses.clone();
        return packet;
    }
}
