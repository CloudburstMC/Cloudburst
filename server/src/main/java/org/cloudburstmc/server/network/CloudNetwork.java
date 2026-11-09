package org.cloudburstmc.server.network;

import com.dosse.upnp.UPnP;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.GlobalEventExecutor;
import lombok.extern.log4j.Log4j2;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.event.server.ServerListPingEvent;
import org.cloudburstmc.netty.channel.nethernet.NetherNetChannelFactory;
import org.cloudburstmc.netty.channel.nethernet.NetherNetChildChannel;
import org.cloudburstmc.netty.channel.nethernet.config.NetherChannelMetrics;
import org.cloudburstmc.netty.channel.nethernet.config.NetherChannelOption;
import org.cloudburstmc.netty.channel.nethernet.signaling.ClientInfo;
import org.cloudburstmc.netty.channel.nethernet.signaling.JoinRefusal;
import org.cloudburstmc.netty.channel.nethernet.signaling.NetherNetHTTPServerSignaling;
import org.cloudburstmc.netty.channel.nethernet.signaling.PongData;
import org.cloudburstmc.netty.signaling.ProviderClient;
import org.cloudburstmc.netty.signaling.provider.ProviderRuntimeConfiguration;
import org.cloudburstmc.netty.util.nethernet.*;
import org.cloudburstmc.protocol.bedrock.BedrockServerSession;
import org.cloudburstmc.server.CloudServer;
import org.cloudburstmc.server.configuration.GlobalConfiguration;
import org.cloudburstmc.server.configuration.type.SignalingMode;
import org.cloudburstmc.server.network.nethernet.CloudExternalSignaling;
import org.cloudburstmc.server.network.nethernet.CloudLoginTimeoutHandler;
import org.cloudburstmc.server.network.nethernet.CloudNetherNetServerInitializer;
import org.cloudburstmc.server.network.nethernet.NetherNetJoinThrottle;
import org.cloudburstmc.server.player.handler.NetworkSettingsPacketHandler;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
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
    private final Set<Integer> mappedMediaPorts = new HashSet<>();

    private volatile CloudServerStatus status;
    private NetherNetHTTPServerSignaling signaling;
    private EventLoopGroup eventLoopGroup;
    private ExecutorService loginExecutor;
    private ExecutorService resourcePackExecutor;
    private CloudConnectionLimiter connectionLimiter;
    private Channel listener;
    private CloudExternalSignaling externalSignaling;
    private boolean tcpPortMapped;

    public CloudNetwork(CloudServer server) {
        this.server = Objects.requireNonNull(server, "server");
    }

    public void bind() throws Exception {
        GlobalConfiguration.Network config = this.server.getConfig().getNetwork();
        if (this.eventLoopGroup != null || this.closed.get()) {
            throw new IllegalStateException("The network listener cannot be bound again");
        }

        if (config.getMaxConnections() <= 0
                || config.getMaxConnectionsPerAddress() <= 0 || config.getMaxPendingJoins() <= 0
                || config.getLoginTimeoutSeconds() <= 0) {
            throw new IllegalArgumentException("Network identity and connection limits must be configured");
        }

        String bindHost = this.server.getIp().isBlank() ? "0.0.0.0" : this.server.getIp();
        InetSocketAddress address = new InetSocketAddress(bindHost, this.server.getPort());
        if (address.getAddress() != null && address.getAddress().isLoopbackAddress()) {
            throw new IllegalArgumentException("NetherNet requires a local network address or a wildcard bind address instead of loopback");
        }

        try {
            this.connectionLimiter = new CloudConnectionLimiter(config.getMaxConnections(), config.getMaxConnectionsPerAddress());

            int loginThreads = Math.clamp(Runtime.getRuntime().availableProcessors(), 1, 8);
            this.loginExecutor = new ThreadPoolExecutor(loginThreads, loginThreads, 0, TimeUnit.SECONDS,
                    new ArrayBlockingQueue<>(config.getMaxPendingJoins()),
                    Thread.ofPlatform().daemon().name("Cloudburst Login #", 0).factory()
            );

            int packThreads = Math.min(loginThreads, 4);
            this.resourcePackExecutor = new ThreadPoolExecutor(packThreads, packThreads, 0, TimeUnit.SECONDS,
                    new ArrayBlockingQueue<>(config.getMaxPendingJoins()),
                    Thread.ofPlatform().daemon().name("Cloudburst Resource Pack #", 0).factory()
            );
            this.refreshStatus();

            this.eventLoopGroup = new NioEventLoopGroup();
            int builtinMediaPort = config.getBuiltin().getMediaPort() == 0 ? address.getPort() : config.getBuiltin().getMediaPort();
            int externalMediaPort = config.getNxs().getMediaPort() == 0 ? address.getPort() : config.getNxs().getMediaPort();

            boolean builtin = config.getSignalingMode() != SignalingMode.NXS;
            boolean external = config.getSignalingMode() != SignalingMode.BUILTIN;
            if (builtin && external && builtinMediaPort == externalMediaPort) {
                throw new IllegalArgumentException("Built-in and external signaling need different local UDP ports in both mode");
            }

            NetherNetJoinThrottle throttle = new NetherNetJoinThrottle(config.getMaxConnectionsPerAddress());
            if (builtin) {
                this.bindBuiltin(config, address, builtinMediaPort, throttle);
            }

            if (external) {
                this.bindExternal(config, bindHost, externalMediaPort, throttle);
            }

            this.openPortMappings(builtin ? builtinMediaPort : 0, external ? externalMediaPort : 0);
        } catch (Exception | LinkageError failure) {
            this.close();
            throw failure;
        }
    }

    private void bindBuiltin(GlobalConfiguration.Network config, InetSocketAddress address, int mediaPort, NetherNetJoinThrottle throttle) throws Exception {
        GlobalConfiguration.BuiltinSignaling builtin = config.getBuiltin();
        if (builtin.getIdentityFile().isBlank()) {
            throw new IllegalArgumentException("The built-in signaling identity file must be configured");
        }

        boolean certificateConfigured = !builtin.getTlsCertificate().isBlank();
        if (!certificateConfigured && (!builtin.getTlsPrivateKey().isBlank() || !builtin.getTlsPassword().isBlank())) {
            throw new IllegalArgumentException("Configure a TLS certificate before setting its private key or password");
        }

        Path directory = this.server.getDataPath();
        OperatorIdentity identity = OperatorIdentity.fromPemOrCreate(directory.resolve(builtin.getIdentityFile()).toFile(), this.server.getName());
        boolean authenticated = this.server.getConfig().isXboxAuth();
        NetherNetHTTPServerSignaling.Builder builder = new NetherNetHTTPServerSignaling.Builder()
                .setIdentity(identity)
                .setTokenTrust(authenticated ? TokenTrust.MINECRAFT_AUTH : TokenTrust.ANY)
                .setMaxConnectionsPerAddress(config.getMaxConnectionsPerAddress())
                .setMaxPendingJoins(config.getMaxPendingJoins())
                .setAnswerTimeoutSeconds(builtin.getHandshakeTimeoutSeconds())
                .setAdvertisedAddresses(builtin.getAdvertisedAddresses())
                .setIceOnLocalPort(mediaPort == address.getPort())
                .setForwardedForProxies(builtin.getForwardedForProxies())
                .setProxyProtocolProxies(builtin.getProxyProtocolProxies())
                .setStunServers(builtin.getStunServers())
                .setMotdProvider(this::queryStatus)
                .setPlayerFilter((host, player) -> this.refuseConnection(player, throttle));

        if (certificateConfigured) {
            String password = SecretValue.resolve(builtin.getTlsPassword(), directory);
            Path certificate = directory.resolve(builtin.getTlsCertificate());
            if (builtin.getTlsPrivateKey().isBlank()) {
                builder.setHttpsKeystore(certificate.toFile(), password);
            } else {
                builder.setHttpsPem(certificate.toFile(), directory.resolve(builtin.getTlsPrivateKey()).toFile(), password.isEmpty() ? null : password);
            }
        }

        this.signaling = builder.build();
        this.listener = this.bootstrap(config, false, authenticated)
                .channelFactory(NetherNetChannelFactory.server(this.signaling))
                .option(NetherChannelOption.NETHER_SERVER_ICE_ADDRESS, new InetSocketAddress(address.getHostString(), mediaPort))
                .option(NetherChannelOption.NETHER_SERVER_PROXY_PROTOCOL, IpRangeSet.parse(config.getMediaProxyRelays()))
                .option(NetherChannelOption.NETHER_SERVER_PREFER_PROXIED_ADDRESS, builtin.isPreferProxiedAddress())
                .option(NetherChannelOption.NETHER_SERVER_RTC_HANDSHAKE_TIMEOUT_SECONDS, builtin.getHandshakeTimeoutSeconds())
                .bind(address).sync().channel();
    }

    private void bindExternal(GlobalConfiguration.Network config, String bindHost, int port, NetherNetJoinThrottle throttle) throws Exception {
        GlobalConfiguration.ExternalSignaling nxs = config.getNxs();
        IpRangeSet relays = IpRangeSet.parse(config.getMediaProxyRelays());
        if (!relays.isEmpty() && nxs.getAdvertisedAddresses().isEmpty()) {
            throw new IllegalArgumentException("NXS requires advertised addresses when media proxy relays are configured");
        }

        ProviderRuntimeConfiguration.Settings settings = ProviderRuntimeConfiguration.Settings.builder(nxs.getEndpoint())
                .token(nxs.getToken())
                .advertiseAddresses(nxs.getAdvertisedAddresses())
                .connectionChecks(nxs.isConnectionChecks())
                .assistedJoins(nxs.isAssistedJoins())
                .stunDiscovery(nxs.isStunDiscovery())
                .deregisterOnShutdown(nxs.isDeregisterOnShutdown())
                .bedrockProtocols(CloudExternalSignaling.protocols())
                .gameOutcomes(true)
                .proxyProtocolRelays(relays)
                .build();
        ProviderRuntimeConfiguration runtime = ProviderRuntimeConfiguration.resolve(settings,
                this.server.getDataPath().resolve("nethernet"), bindHost, port, this.server.getMaxPlayers());
        ServerBootstrap bootstrap = this.bootstrap(config, true, false)
                .option(NetherChannelOption.NETHER_SERVER_THROTTLE, throttle);
        this.externalSignaling = CloudExternalSignaling.open(bootstrap, runtime,
                () -> this.status, this.acceptingConnections::get,
                new ProviderClient.Software(this.server.getName(), this.server.getImplementationVersion())
        );
    }

    private ServerBootstrap bootstrap(GlobalConfiguration.Network config, boolean external, boolean authenticated) {
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

        return new ServerBootstrap().group(this.eventLoopGroup)
                .childOption(NetherChannelOption.NETHER_METRICS, metrics)
                .childHandler(new ChannelInitializer<NetherNetChildChannel>() {
                    @Override
                    protected void initChannel(NetherNetChildChannel channel) {
                        if (!CloudNetwork.this.acceptingConnections.get()) {
                            channel.close();
                            return;
                        }

                        if (external) {
                            // Assisted joins may not have a verified remote address until ICE connects.
                            channel.pipeline().addLast(new ChannelInboundHandlerAdapter() {
                                @Override
                                public void channelActive(ChannelHandlerContext context) {
                                    if (CloudNetwork.this.admit(channel, channel.reportedAddress().address())) {
                                        context.fireChannelActive();
                                    }
                                }
                            });
                        } else {
                            PlayerInfo player = channel.attr(NetherNetChildChannel.PLAYER_INFO).get();
                            if (!CloudNetwork.this.admit(channel, player == null ? null : player.remoteAddress())) {
                                return;
                            }
                        }

                        channel.pipeline().addLast(new CloudLoginTimeoutHandler(config.getLoginTimeoutSeconds()));
                        channel.pipeline().addLast(new CloudNetherNetServerInitializer(session -> CloudNetwork.this.initializeSession(session, authenticated)));
                        if (external) {
                            channel.pipeline().addLast(CloudNetwork.this.externalSignaling.observeRejections());
                        }
                    }
                });
    }

    public void startAcceptingConnections() {
        if (this.closed.get() || !(this.listener != null && this.listener.isActive()
                || this.externalSignaling != null && this.externalSignaling.channel().isActive())) {
            throw new IllegalStateException("The network listener is not bound");
        }

        if (this.acceptingConnections.compareAndSet(false, true) && this.externalSignaling != null) {
            this.externalSignaling.start();
        }
    }

    public void stopAcceptingConnections() {
        this.acceptingConnections.set(false);
    }

    private @Nullable JoinRefusal refuseConnection(PlayerInfo player, NetherNetJoinThrottle joinThrottle) {
        if (!this.acceptingConnections.get()) {
            return JoinRefusal.ERROR;
        }

        if (this.connectionLimiter.isFull()) {
            return JoinRefusal.FULL;
        }

        return joinThrottle.refuse(player.remoteAddress());
    }

    private boolean admit(NetherNetChildChannel channel, @Nullable InetSocketAddress address) {
        InetAddress ip = address == null ? null : address.getAddress();
        if (!this.acceptingConnections.get() || ip == null || !this.connectionLimiter.tryAcquire(ip)) {
            this.reportExternalRejection(channel, "server_rejected");
            channel.close();
            return false;
        }

        channel.closeFuture().addListener(future -> this.connectionLimiter.release(ip));
        this.connections.add(channel);
        return true;
    }

    private void initializeSession(BedrockServerSession session, boolean authenticatedTransport) {
        if (!this.acceptingConnections.get()) {
            session.getPeer().getChannel().close();
            return;
        }

        session.setLogging(false);
        session.setPacketHandler(new NetworkSettingsPacketHandler(session, this.server, authenticatedTransport));
    }

    public void reportExternalJoin(Channel channel) {
        if (this.externalSignaling != null) {
            this.externalSignaling.reportJoined(channel);
        }
    }

    public void reportExternalRejection(Channel channel, String reason) {
        if (this.externalSignaling != null) {
            this.externalSignaling.reportRejected(channel, reason);
        }
    }

    public boolean executeLogin(Runnable login) {
        return this.execute(this.loginExecutor, login);
    }

    public boolean executeResourcePackRead(Runnable read) {
        return this.execute(this.resourcePackExecutor, read);
    }

    private boolean execute(ExecutorService executor, Runnable task) {
        if (!this.acceptingConnections.get()) {
            return false;
        }

        try {
            executor.execute(task);
            return true;
        } catch (RejectedExecutionException rejected) {
            return false;
        }
    }

    public void refreshStatus() {
        if (!this.server.isPrimaryThread()) {
            throw new IllegalStateException("Server status must be refreshed on the server thread");
        }

        CloudServerStatus previous = this.status;
        this.status = new CloudServerStatus(this.server.motd(),
                this.server.getDefaultLevel() == null ? this.server.getConfig().getDefaultLevel() : this.server.getDefaultLevel().getName(),
                this.server.getDefaultGamemode(),
                this.server.getOnlinePlayers().size(), this.server.getMaxPlayers(),
                this.server.getConfig().isHardcore(), this.server.getConfig().isXboxAuth());
        if (this.externalSignaling != null && this.acceptingConnections.get() && !this.status.equals(previous)) {
            this.externalSignaling.refreshStatus();
        }
    }

    private PongData queryStatus(String hostname, InetSocketAddress address, @Nullable ClientInfo clientInfo) {
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

    public long getBytesSent() {
        return this.upload.sum();
    }

    public long getBytesReceived() {
        return this.download.sum();
    }

    private void openPortMappings(int builtinMediaPort, int externalMediaPort) {
        if (!this.server.getConfig().getSettings().isUpnp()) {
            return;
        }

        if (!UPnP.isUPnPAvailable()) {
            log.warn("UPnP is unavailable. Forward the configured signaling and media ports manually");
            return;
        }

        if (builtinMediaPort != 0) {
            this.tcpPortMapped = UPnP.openPortTCP(this.server.getPort(), this.server.getName());
            if (!this.tcpPortMapped) {
                log.warn("UPnP could not forward signaling on TCP port {}", this.server.getPort());
            }
            this.mapMediaPort(builtinMediaPort);
        }

        if (externalMediaPort != 0) {
            this.mapMediaPort(externalMediaPort);
        }
    }

    private void mapMediaPort(int port) {
        if (UPnP.openPortUDP(port, this.server.getName())) {
            this.mappedMediaPorts.add(port);
        } else {
            log.warn("UPnP could not forward NetherNet media on UDP port {}", port);
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

        if (this.resourcePackExecutor != null) {
            this.resourcePackExecutor.shutdownNow();
        }

        try {
            if (this.externalSignaling != null) {
                this.externalSignaling.close();
            }

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
                        this.mappedMediaPorts.forEach(UPnP::closePortUDP);
                    }
                }
            }
        }
    }
}
