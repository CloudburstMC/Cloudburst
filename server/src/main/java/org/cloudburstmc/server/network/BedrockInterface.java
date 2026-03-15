package org.cloudburstmc.server.network;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollDatagramChannel;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.kqueue.KQueue;
import io.netty.channel.kqueue.KQueueDatagramChannel;
import io.netty.channel.kqueue.KQueueEventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import lombok.extern.log4j.Log4j2;
import org.cloudburstmc.api.event.server.QueryRegenerateEvent;
import org.cloudburstmc.netty.channel.raknet.RakChannelFactory;
import org.cloudburstmc.netty.channel.raknet.RakChildChannel;
import org.cloudburstmc.netty.channel.raknet.RakServerChannel;
import org.cloudburstmc.netty.channel.raknet.RakState;
import org.cloudburstmc.netty.channel.raknet.config.RakChannelOption;
import org.cloudburstmc.netty.channel.raknet.config.RakServerCookieMode;
import org.cloudburstmc.netty.channel.raknet.config.RakServerMetrics;
import org.cloudburstmc.netty.handler.codec.raknet.server.RakServerRateLimiter;
import org.cloudburstmc.protocol.adventure.AdventureTextConverter;
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
    private final EventLoopGroup eventLoopGroup;
    private final List<Channel> channels = new ArrayList<>();
    private final BedrockPong advertisement = new BedrockPong();
    private Network network;

    public BedrockInterface(CloudServer server) {
        this.server = server;

        Class<? extends DatagramChannel> datagramChannelClass;
        if (Epoll.isAvailable()) {
            this.eventLoopGroup = new EpollEventLoopGroup();
            datagramChannelClass = EpollDatagramChannel.class;
            log.debug("Using Epoll transport");
        } else if (KQueue.isAvailable()) {
            this.eventLoopGroup = new KQueueEventLoopGroup();
            datagramChannelClass = KQueueDatagramChannel.class;
            log.debug("Using KQueue transport");
        } else {
            this.eventLoopGroup = new NioEventLoopGroup();
            datagramChannelClass = NioDatagramChannel.class;
            log.debug("Using NIO transport");
        }

        RakServerMetrics metrics = new RakServerMetrics() {
            @Override
            public void invalidCookie(InetSocketAddress address) {
                log.debug("[{}] Rejected connection: invalid or expired stateless cookie (possible IP spoofing / DDoS)", address);
            }

            @Override
            public void channelOpen(InetSocketAddress address) {
                log.trace("[{}] RakNet channel opened", address);
            }

            @Override
            public void channelClose(InetSocketAddress address) {
                log.trace("[{}] RakNet channel closed", address);
            }

            @Override
            public void addressBlocked(InetAddress address) {
                log.debug("[{}] Address blocked", address);
            }

            @Override
            public void addressUnblocked(InetAddress address) {
                log.debug("[{}] Address unblocked", address);
            }

            @Override
            public void bytesIn(RakChildChannel channel, int count) {
                if (BedrockInterface.this.network != null) {
                    BedrockInterface.this.network.addStatistics(0, count);
                }
            }

            @Override
            public void bytesOut(RakChildChannel channel, int count) {
                if (BedrockInterface.this.network != null) {
                    BedrockInterface.this.network.addStatistics(count, 0);
                }
            }

            @Override
            public void stateChange(RakChildChannel channel, RakState state) {
                log.trace("[{}] RakNet state -> {}", channel.remoteAddress(), state);
            }
        };

        ServerBootstrap bootstrap = new ServerBootstrap()
                .channelFactory(RakChannelFactory.server(datagramChannelClass))
                .group(this.eventLoopGroup)
                .option(RakChannelOption.RAK_SERVER_COOKIE_MODE, RakServerCookieMode.ACTIVE)
                .option(RakChannelOption.RAK_SERVER_METRICS, metrics)
                .childHandler(new BedrockServerInitializer() {
                    @Override
                    protected void initSession(BedrockServerSession session) {
                        session.getPeer().getCodecHelper().setTextConverter(new AdventureTextConverter());
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
        for (Channel channel : this.channels) {
            if (channel instanceof RakServerChannel) {
                ((RakServerChannel) channel).tryBlockAddress(address, -1, TimeUnit.MILLISECONDS);
            }
        }
    }

    @Override
    public void blockAddress(InetAddress address, long timeout, TimeUnit unit) {
        for (Channel channel : this.channels) {
            if (channel instanceof RakServerChannel) {
                ((RakServerChannel) channel).tryBlockAddress(address, timeout, unit);
            }
        }
    }

    @Override
    public void unblockAddress(InetAddress address) {
        for (Channel channel : this.channels) {
            if (channel instanceof RakServerChannel) {
                RakServerRateLimiter rateLimiter = channel.pipeline().get(RakServerRateLimiter.class);
                if (rateLimiter != null) {
                    rateLimiter.unblockAddress(address);
                }
            }
        }
    }

    @Override
    public void setNetwork(Network network) {
        this.network = network;
    }

    @Override
    public void sendRawPacket(InetSocketAddress socketAddress, ByteBuf payload) {
    }

    @Override
    public void setName(String name) {
        QueryRegenerateEvent info = this.server.getQueryInformation();
        String[] names = name.split("!@#"); // Split double names within the program
        String motd = Utils.rtrim(names[0].replace(";", "\\;"), '\\');
        String subMotd = names.length > 1 ? Utils.rtrim(names[1].replace(";", "\\;"), '\\') : "";
        String gm = this.server.getDefaultGamemode().getName();

        this.advertisement.edition("MCPE")
                .motd(motd)
                .subMotd(subMotd.trim().isEmpty() ? "Cloudburst" : subMotd)
                .playerCount(info.getPlayerCount())
                .maximumPlayerCount(info.getMaxPlayerCount())
                .version(ProtocolInfo.getDefaultMinecraftVersion())
                .protocolVersion(ProtocolInfo.getDefaultProtocolVersion())
                .gameType(gm.substring(0, 1).toUpperCase() + gm.substring(1))
                .nintendoLimited(false)
                .ipv4Port(this.server.getPort())
                .serverId(this.server.getServerUniqueId().getMostSignificantBits());

        for (Channel channel : this.channels) {
            channel.config().setOption(RakChannelOption.RAK_ADVERTISEMENT, this.advertisement.toByteBuf());
        }
    }

    @Override
    public boolean process() {
        return true;
    }

    @Override
    public void shutdown() {
        for (Channel channel : this.channels) {
            channel.close().awaitUninterruptibly();
        }
        this.eventLoopGroup.shutdownGracefully(0, 2, TimeUnit.SECONDS).awaitUninterruptibly();
    }

    @Override
    public void emergencyShutdown() {
        this.shutdown();
    }
}
