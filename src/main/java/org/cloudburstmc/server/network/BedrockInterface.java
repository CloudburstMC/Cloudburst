package org.cloudburstmc.server.network;

import com.nukkitx.network.util.DisconnectReason;
import com.nukkitx.protocol.bedrock.BedrockPong;
import com.nukkitx.protocol.bedrock.BedrockServer;
import com.nukkitx.protocol.bedrock.BedrockServerEventHandler;
import com.nukkitx.protocol.bedrock.BedrockServerSession;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.cloudburstmc.api.event.server.QueryRegenerateEvent;
import org.cloudburstmc.server.CloudServer;
import org.cloudburstmc.server.network.query.QueryHandler;
import org.cloudburstmc.server.player.CloudPlayer;
import org.cloudburstmc.server.player.handler.LoginPacketHandler;
import org.cloudburstmc.server.utils.Utils;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Queue;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Log4j2
@ParametersAreNonnullByDefault
public class BedrockInterface implements BedrockServerEventHandler, SourceInterface {

    private final CloudServer server;

    private final QueryHandler queryHandler;

    private final BedrockServer bedrock;
    private final BedrockPong advertisement = new BedrockPong();
    private final Queue<NukkitSessionListener> disconnectQueue = new ConcurrentLinkedQueue<>();

    public BedrockInterface(CloudServer server) throws Exception {
        this.server = server;
        this.queryHandler = server.getNetwork().getQueryHandler();

        InetSocketAddress bindAddress = new InetSocketAddress(this.server.getIp(), this.server.getPort());

        this.bedrock = new BedrockServer(bindAddress, Runtime.getRuntime().availableProcessors());
        this.bedrock.setHandler(this);
        try {
            this.bedrock.bind().join();
        } catch (CompletionException e) {
            if (e.getCause() instanceof Exception) {
                throw (Exception) e.getCause();
            }
            throw e;
        }
    }

    @Override
    public boolean onConnectionRequest(InetSocketAddress inetSocketAddress) {
        return true; // TODO: 29/01/2020 Add an event?
    }

    @Nullable
    @Override
    public BedrockPong onQuery(InetSocketAddress inetSocketAddress) {
        return advertisement;
    }

    @Override
    public void onSessionCreation(BedrockServerSession session) {
        session.setLogging(false);
        session.setPacketHandler(new LoginPacketHandler(session, server, this));
    }

    @Override
    public void blockAddress(InetAddress address) {
        this.bedrock.getRakNet().block(address);
    }

    @Override
    public void blockAddress(InetAddress address, long timeout, TimeUnit unit) {
        this.bedrock.getRakNet().block(address, timeout, unit);
    }

    @Override
    public void unblockAddress(InetAddress address) {
        this.bedrock.getRakNet().unblock(address);
    }

    @Override
    public void setNetworkManager(NetworkManager networkManager) {
        // no-op
    }

    @Override
    public void sendRawPacket(InetSocketAddress socketAddress, ByteBuf payload) {
        this.bedrock.getRakNet().send(socketAddress, payload);
    }

    @Override
    public void onUnhandledDatagram(ChannelHandlerContext ctx, DatagramPacket packet) {
        this.handlePacket(packet.sender(), packet.content());
    }

    public void handlePacket(InetSocketAddress address, ByteBuf payload) {
        try {
            if (!payload.isReadable(3)) {
                return;
            }
            byte[] prefix = new byte[2];
            payload.readBytes(prefix);

            if (!Arrays.equals(prefix, new byte[]{(byte) 0xfe, (byte) 0xfd})) {
                return;
            }
            if (this.queryHandler != null) {
                this.queryHandler.handle(address, payload);
            }
        } catch (Exception e) {
            log.error("Error whilst handling packet", e);
            this.server.getNetwork().blockAddress(address.getAddress());
        }
    }

    @Override
    public void setMotd(String motd0, String subMotd0) {
        QueryRegenerateEvent info = this.server.getQueryInformation();
        String motd = Utils.rtrim(motd0.replace(";", "\\;"), '\\');
        String subMotd = Utils.rtrim(subMotd0.replace(";", "\\;"), '\\');
        String gm = this.server.getDefaultGamemode().getName();

        this.advertisement.setEdition("MCPE");
        this.advertisement.setMotd(motd);
        this.advertisement.setSubMotd(subMotd.trim().isEmpty() ? "Cloudburst" : subMotd);
        this.advertisement.setPlayerCount(info.getPlayerCount());
        this.advertisement.setMaximumPlayerCount(info.getMaxPlayerCount());
        this.advertisement.setVersion("1");
        this.advertisement.setProtocolVersion(0);
        this.advertisement.setGameType(gm.substring(0, 1).toUpperCase() + gm.substring(1));
        this.advertisement.setNintendoLimited(false);
        this.advertisement.setIpv4Port(this.server.getPort());
        this.advertisement.setIpv6Port(this.server.getPort());
    }

    @Override
    public boolean process() {
        NukkitSessionListener listener;
        while ((listener = disconnectQueue.poll()) != null) {
            listener.player.close(listener.player.getLeaveMessage(), listener.disconnectReason, false);
        }
        return true;
    }

    @Override
    public void shutdown() {
        this.bedrock.close();
    }

    @Override
    public void emergencyShutdown() {
        this.bedrock.close();
    }

    public NukkitSessionListener initDisconnectHandler(CloudPlayer player) {
        return new NukkitSessionListener(player);
    }

    @RequiredArgsConstructor
    private class NukkitSessionListener implements Consumer<DisconnectReason> {
        private final CloudPlayer player;
        private String disconnectReason = null;

        @Override
        public void accept(DisconnectReason disconnectReason) {
            if (disconnectReason == DisconnectReason.TIMED_OUT) {
                this.disconnectReason = "Timed out";
            } else {
                this.disconnectReason = "Disconnected from Server";
            }
            // Queue for disconnect on main thread.
            BedrockInterface.this.disconnectQueue.add(this);
        }
    }
}
