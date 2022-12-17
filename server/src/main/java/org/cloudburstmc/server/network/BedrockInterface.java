package org.cloudburstmc.server.network;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import lombok.extern.log4j.Log4j2;
import org.cloudburstmc.api.event.server.QueryRegenerateEvent;
import org.cloudburstmc.netty.channel.raknet.RakChannelFactory;
import org.cloudburstmc.netty.channel.raknet.config.RakChannelOption;
import org.cloudburstmc.protocol.bedrock.BedrockPong;
import org.cloudburstmc.protocol.bedrock.BedrockServerSession;
import org.cloudburstmc.protocol.bedrock.netty.initializer.BedrockServerInitializer;
import org.cloudburstmc.server.CloudServer;
import org.cloudburstmc.server.player.handler.LoginPacketHandler;
import org.cloudburstmc.server.utils.Utils;

import javax.annotation.ParametersAreNonnullByDefault;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Log4j2
@ParametersAreNonnullByDefault
public class BedrockInterface implements AdvancedSourceInterface {

    private final CloudServer server;

    private final List<Channel> channels = new ArrayList<>();
    private final BedrockPong advertisement = new BedrockPong();

    public BedrockInterface(CloudServer server) throws Exception {
        this.server = server;

        ServerBootstrap bootstrap = new ServerBootstrap()
                .channelFactory(RakChannelFactory.server(NioDatagramChannel.class)) // TODO: Epoll, KQueue and IO Uring support
                .group(new NioEventLoopGroup())
                .childHandler(new BedrockServerInitializer() {
                    @Override
                    protected void initSession(BedrockServerSession session) {
                        session.setLogging(false);
                        session.setPacketHandler(new LoginPacketHandler(session, server, BedrockInterface.this));
                    }
                })
                .localAddress(this.server.getIp(), this.server.getPort());

        this.channels.add(bootstrap.bind()
                .awaitUninterruptibly()
                .channel());
    }

    @Override
    public void blockAddress(InetAddress address) {
    }

    @Override
    public void blockAddress(InetAddress address, long timeout, TimeUnit unit) {
    }

    @Override
    public void unblockAddress(InetAddress address) {
    }

    @Override
    public void setNetwork(Network network) {
        // no-op
    }

    @Override
    public void sendRawPacket(InetSocketAddress socketAddress, ByteBuf payload) {
    }

    @Override
    public void setName(String name) {
        QueryRegenerateEvent info = this.server.getQueryInformation();
        String[] names = name.split("!@#");  //Split double names within the program
        String motd = Utils.rtrim(names[0].replace(";", "\\;"), '\\');
        String subMotd = names.length > 1 ? Utils.rtrim(names[1].replace(";", "\\;"), '\\') : "";
        String gm = this.server.getDefaultGamemode().getName();

        this.advertisement.edition("MCPE")
                .motd(motd)
                .subMotd(subMotd.trim().isEmpty() ? "Cloudburst" : subMotd)
                .playerCount(info.getPlayerCount())
                .maximumPlayerCount(info.getMaxPlayerCount())
                .version("")
                .protocolVersion(0)
                .gameType(gm.substring(0, 1).toUpperCase() + gm.substring(1))
                .nintendoLimited(false);

        for (Channel channel : this.channels) {
            channel.config().setOption(RakChannelOption.RAK_ADVERTISEMENT, this.advertisement.toByteBuf());
        }
    }

    @Override
    public boolean process() {
//        NukkitSessionListener listener;
//        while ((listener = disconnectQueue.poll()) != null) {
//            listener.player.close(listener.player.getLeaveMessage(), listener.disconnectReason, false);
//        }
        return true;
    }

    @Override
    public void shutdown() {
        for (Channel channel : this.channels) {
            channel.close().awaitUninterruptibly();
        }
    }

    @Override
    public void emergencyShutdown() {
        this.shutdown();
    }
//
//    @RequiredArgsConstructor
//    private class NukkitSessionListener implements Consumer<DisconnectReason> {
//        private final CloudPlayer player;
//        private String disconnectReason = null;
//
//        @Override
//        public void accept(DisconnectReason disconnectReason) {
//            if (disconnectReason == DisconnectReason.TIMED_OUT) {
//                this.disconnectReason = "Timed out";
//            } else {
//                this.disconnectReason = "Disconnected from Server";
//            }
//            // Queue for disconnect on main thread.
//            BedrockInterface.this.disconnectQueue.add(this);
//        }
//    }
}
