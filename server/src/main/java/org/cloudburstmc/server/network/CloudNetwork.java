package org.cloudburstmc.server.network;

import com.dosse.upnp.UPnP;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.GlobalEventExecutor;
import lombok.extern.log4j.Log4j2;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.cloudburstmc.api.event.server.ServerListPingEvent;
import org.cloudburstmc.netty.channel.nethernet.NetherNetChannelFactory;
import org.cloudburstmc.netty.channel.nethernet.NetherNetChildChannel;
import org.cloudburstmc.netty.channel.nethernet.config.NetherChannelMetrics;
import org.cloudburstmc.netty.channel.nethernet.config.NetherChannelOption;
import org.cloudburstmc.netty.channel.nethernet.signaling.JoinRefusal;
import org.cloudburstmc.netty.channel.nethernet.signaling.NetherNetHTTPServerSignaling;
import org.cloudburstmc.netty.channel.nethernet.signaling.PongData;
import org.cloudburstmc.netty.util.nethernet.OperatorIdentity;
import org.cloudburstmc.netty.util.nethernet.PlayerInfo;
import org.cloudburstmc.netty.util.nethernet.TokenTrust;
import org.cloudburstmc.protocol.bedrock.BedrockServerSession;
import org.cloudburstmc.server.CloudServer;
import org.cloudburstmc.server.config.ServerConfig;
import org.cloudburstmc.server.network.nethernet.CloudLoginTimeoutHandler;
import org.cloudburstmc.server.network.nethernet.CloudNetherNetServerInitializer;
import org.cloudburstmc.server.player.handler.NetworkSettingsPacketHandler;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.security.SecureRandom;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.LongAdder;

@Log4j2
public class CloudNetwork implements AutoCloseable {

    private final CloudServer server;
    private final ChannelGroup connections = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE, true);
    private final AtomicBoolean acceptingConnections = new AtomicBoolean();
    private final AtomicBoolean closed = new AtomicBoolean();
    private final LongAdder upload = new LongAdder();
    private final LongAdder download = new LongAdder();
    private final String nonce = String.format("%016x", new SecureRandom().nextLong());

    private volatile CloudServerStatus status;
    private NetherNetHTTPServerSignaling signaling;
    private EventLoopGroup eventLoopGroup;
    private ExecutorService loginExecutor;
    private CloudConnectionLimiter connectionLimiter;
    private Channel listener;
    private boolean tcpPortMapped;
    private boolean udpPortMapped;

    public CloudNetwork(CloudServer server) {
        this.server = Objects.requireNonNull(server, "server");
    }

    public void bind() throws Exception {
        ServerConfig.Network config = this.server.getConfig().getNetwork();
        if (this.listener != null || this.closed.get()) {
            throw new IllegalStateException("The network listener cannot be bound again");
        }

        if (config.getIdentityFile().isBlank() || config.getMaxConnections() <= 0
                || config.getMaxConnectionsPerAddress() <= 0 || config.getMaxPendingJoins() <= 0
                || config.getHandshakeTimeoutSeconds() <= 0 || config.getLoginTimeoutSeconds() <= 0) {
            throw new IllegalArgumentException("Network identity and connection limits must be configured");
        }

        boolean certificateConfigured = !config.getTlsCertificate().isBlank();
        if (certificateConfigured == config.getTlsPrivateKey().isBlank()) {
            throw new IllegalArgumentException("Both the TLS certificate and private key must be configured");
        }

        try {
            this.connectionLimiter = new CloudConnectionLimiter(config.getMaxConnections(), config.getMaxConnectionsPerAddress());
            int loginThreads = Math.clamp(Runtime.getRuntime().availableProcessors(), 1, 8);
            this.loginExecutor = new ThreadPoolExecutor(loginThreads, loginThreads, 0, TimeUnit.SECONDS,
                    new ArrayBlockingQueue<>(config.getMaxPendingJoins()),
                    Thread.ofPlatform().daemon().name("Cloudburst Login #", 0).factory());
            this.refreshStatus();

            OperatorIdentity identity = OperatorIdentity.fromPemOrCreate(
                    this.server.getDataPath().resolve(config.getIdentityFile()).toFile(),
                    this.server.getName()
            );

            NetherNetHTTPServerSignaling.Builder builder = new NetherNetHTTPServerSignaling.Builder()
                    .setIdentity(identity)
                    .setTokenTrust(this.server.getConfig().isXboxAuth() ? TokenTrust.MINECRAFT_AUTH : TokenTrust.ANY)
                    .setMaxConnectionsPerAddress(config.getMaxConnectionsPerAddress())
                    .setMaxPendingJoins(config.getMaxPendingJoins())
                    .setAnswerTimeoutSeconds(config.getHandshakeTimeoutSeconds())
                    .setAdvertisedAddresses(config.getAdvertisedAddresses())
                    .setMotdProvider(this::queryStatus)
                    .setPlayerFilter((host, player) -> this.refuseConnection());

            if (certificateConfigured) {
                builder.setHttpsPem(this.server.getDataPath().resolve(config.getTlsCertificate()).toFile(),
                        this.server.getDataPath().resolve(config.getTlsPrivateKey()).toFile());
            }

            this.signaling = builder.build();
            this.eventLoopGroup = new NioEventLoopGroup();

            NetherChannelMetrics metrics = new NetherChannelMetrics() {
                @Override
                public void bytesIn(int count) {
                    CloudNetwork.this.download.add(count);
                }

                @Override
                public void bytesOut(int count) {
                    CloudNetwork.this.upload.add(count);
                }
            };

            InetSocketAddress address = new InetSocketAddress(this.server.getIp(), this.server.getPort());
            this.listener = new ServerBootstrap()
                    .group(this.eventLoopGroup)
                    .channelFactory(NetherNetChannelFactory.server(this.signaling))
                    .option(NetherChannelOption.NETHER_SERVER_ICE_ADDRESS, address)
                    .option(NetherChannelOption.NETHER_SERVER_RTC_HANDSHAKE_TIMEOUT_SECONDS,
                            config.getHandshakeTimeoutSeconds())
                    .childOption(NetherChannelOption.NETHER_METRICS, metrics)
                    .childHandler(new ChannelInitializer<NetherNetChildChannel>() {
                        @Override
                        protected void initChannel(NetherNetChildChannel channel) {
                            PlayerInfo player = channel.attr(NetherNetChildChannel.PLAYER_INFO).get();
                            InetAddress address = player == null || player.remoteAddress() == null ? null : player.remoteAddress().getAddress();

                            if (!CloudNetwork.this.acceptingConnections.get() || address == null) {
                                channel.close();
                                return;
                            }

                            if (!CloudNetwork.this.connectionLimiter.tryAcquire(address)) {
                                channel.close();
                                return;
                            }

                            channel.closeFuture().addListener(future -> CloudNetwork.this.connectionLimiter.release(address));
                            CloudNetwork.this.connections.add(channel);
                            channel.pipeline().addLast(new CloudLoginTimeoutHandler(config.getLoginTimeoutSeconds()));
                            channel.pipeline().addLast(new CloudNetherNetServerInitializer(CloudNetwork.this::initializeSession));
                        }
                    })
                    .bind(address).sync().channel();
            this.openPortMappings();
        } catch (Exception | LinkageError failure) {
            this.close();
            throw failure;
        }
    }

    public void startAcceptingConnections() {
        if (this.listener == null || !this.listener.isActive() || this.closed.get()) {
            throw new IllegalStateException("The network listener is not bound");
        }

        this.acceptingConnections.set(true);
    }

    public void stopAcceptingConnections() {
        this.acceptingConnections.set(false);
    }

    private JoinRefusal refuseConnection() {
        if (!this.acceptingConnections.get()) {
            return JoinRefusal.ERROR;
        }

        if (this.connectionLimiter.isFull()) {
            return JoinRefusal.FULL;
        }

        return null;
    }

    private void initializeSession(BedrockServerSession session) {
        if (!this.acceptingConnections.get()) {
            session.getPeer().getChannel().close();
            return;
        }

        session.setLogging(false);
        session.setPacketHandler(new NetworkSettingsPacketHandler(session, this.server));
    }

    public boolean executeLogin(Runnable login) {
        if (!this.acceptingConnections.get()) {
            return false;
        }

        try {
            this.loginExecutor.execute(login);
            return true;
        } catch (RejectedExecutionException rejected) {
            return false;
        }
    }

    public void refreshStatus() {
        if (!this.server.isPrimaryThread()) {
            throw new IllegalStateException("Server status must be refreshed on the server thread");
        }

        this.status = new CloudServerStatus(this.server.motd(),
                this.server.getDefaultLevel() == null ? this.server.getConfig().getDefaultLevel() : this.server.getDefaultLevel().getName(),
                this.server.getDefaultGamemode(),
                this.server.getOnlinePlayers().size(), this.server.getMaxPlayers(),
                this.server.getConfig().isHardcore(), this.server.getConfig().isXboxAuth());
    }

    private PongData queryStatus(String hostname, InetSocketAddress address) {
        CloudServerStatus snapshot = this.status;
        ServerListPingEvent event = new ServerListPingEvent(Objects.requireNonNullElse(hostname, ""), address, snapshot.motd(), snapshot.levelName(),
                snapshot.gameMode(), snapshot.playerCount(), snapshot.maxPlayerCount(), snapshot.hardcore());
        this.server.getEventManager().fire(event);
        if (event.isCancelled()) {
            return null;
        }

        return new PongData.Builder()
                .setServerName(PlainTextComponentSerializer.plainText().serialize(event.motd()))
                .setProtocol(ProtocolInfo.getDefaultProtocolVersion())
                .setVersion(ProtocolInfo.getDefaultMinecraftVersion())
                .setLevelName(event.getLevelName())
                .setGameType(event.getGameMode().getVanillaId())
                .setPlayerCount(event.getPlayerCount())
                .setMaxPlayerCount(event.getMaxPlayerCount())
                .setIsHardcore(event.isHardcore())
                .setOnlineAuth(snapshot.onlineAuth())
                .setSelfSignedAuth(!snapshot.onlineAuth())
                .setNonce(this.nonce)
                .build();
    }

    public double getUpload() {
        return this.upload.doubleValue();
    }

    public double getDownload() {
        return this.download.doubleValue();
    }

    public void resetStatistics() {
        this.upload.sumThenReset();
        this.download.sumThenReset();
    }

    private void openPortMappings() {
        if (!this.server.getConfig().getSettings().isUpnp()) {
            return;
        }

        if (!UPnP.isUPnPAvailable()) {
            log.warn("UPnP is unavailable. Forward both TCP and UDP port {} manually", this.server.getPort());
            return;
        }

        this.tcpPortMapped = UPnP.openPortTCP(this.server.getPort(), this.server.getName());
        this.udpPortMapped = UPnP.openPortUDP(this.server.getPort(), this.server.getName());
        if (!this.tcpPortMapped || !this.udpPortMapped) {
            log.warn("UPnP could not forward both TCP and UDP port {}", this.server.getPort());
        }
    }

    @Override
    public void close() {
        if (!this.closed.compareAndSet(false, true)) {
            return;
        }

        this.acceptingConnections.set(false);
        if (this.loginExecutor != null) {
            this.loginExecutor.shutdownNow();
        }

        try {
            if (this.listener != null) {
                this.listener.close().awaitUninterruptibly();
            } else if (this.signaling != null) {
                this.signaling.close();
            }
        } finally {
            try {
                this.connections.close().awaitUninterruptibly();
            } finally {
                try {
                    if (this.eventLoopGroup != null) {
                        this.eventLoopGroup.shutdownGracefully(0, 2, TimeUnit.SECONDS).awaitUninterruptibly();
                    }
                } finally {
                    try {
                        if (this.tcpPortMapped) {
                            UPnP.closePortTCP(this.server.getPort());
                        }
                    } finally {
                        if (this.udpPortMapped) {
                            UPnP.closePortUDP(this.server.getPort());
                        }
                    }
                }
            }
        }
    }
}
