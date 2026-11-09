package org.cloudburstmc.server.network.nethernet;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.util.AttributeKey;
import lombok.extern.log4j.Log4j2;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.netty.signaling.ProviderClient;
import org.cloudburstmc.netty.signaling.ProviderClient.OperatorAction;
import org.cloudburstmc.netty.signaling.ProviderDiagnostic;
import org.cloudburstmc.netty.signaling.ProviderStateStore;
import org.cloudburstmc.netty.signaling.ServerStatus;
import org.cloudburstmc.netty.signaling.admission.AdmissionPrincipal;
import org.cloudburstmc.netty.signaling.provider.NativeProviderHostFactory;
import org.cloudburstmc.netty.signaling.provider.ProviderRuntimeConfiguration;
import org.cloudburstmc.protocol.bedrock.codec.BedrockCodec;
import org.cloudburstmc.protocol.bedrock.netty.BedrockPacketWrapper;
import org.cloudburstmc.protocol.bedrock.packet.DisconnectPacket;
import org.cloudburstmc.server.network.CloudServerStatus;
import org.cloudburstmc.server.network.ProtocolInfo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * Owns the NXS registration and its native gameplay listener.
 */
@Log4j2
public class CloudExternalSignaling implements AutoCloseable {

    private static final int OPEN_TIMEOUT_SECONDS = 30;
    private static final int STOP_TIMEOUT_SECONDS = 20;
    private static final AttributeKey<Boolean> OUTCOME_REPORTED = AttributeKey.valueOf(CloudExternalSignaling.class, "outcomeReported");

    private final ProviderRuntimeConfiguration runtime;
    private final NativeProviderHostFactory.Host host;
    private final ProviderClient client;
    private final AtomicBoolean stopping = new AtomicBoolean();

    private CloudExternalSignaling(ProviderRuntimeConfiguration runtime, NativeProviderHostFactory.Host host, ProviderClient client) {
        this.runtime = runtime;
        this.host = host;
        this.client = client;
    }

    /**
     * Opens the gameplay listener without registering it with the provider yet.
     */
    public static CloudExternalSignaling open(ServerBootstrap bootstrap, ProviderRuntimeConfiguration runtime,
                                              Supplier<CloudServerStatus> status, BooleanSupplier accepting,
                                              ProviderClient.Software software) throws Exception {
        ProviderStateStore store = new ProviderStateStore(runtime.stateDirectory());
        NativeProviderHostFactory.Host host = null;
        try {
            CompletableFuture<NativeProviderHostFactory.Host> opening =
                    NativeProviderHostFactory.open(bootstrap, runtime).toCompletableFuture();
            try {
                host = opening.get(OPEN_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            } catch (InterruptedException | TimeoutException failure) {
                opening.thenAccept(late -> late.transport().close());
                throw failure;
            }

            host.warnings().forEach(log::warn);
            ProviderClient client = new ProviderClient(runtime.clientConfiguration(), store, host.transport(),
                    () -> listing(status.get()),
                    () -> health(status.get(), accepting.getAsBoolean(), runtime.capacity(), software),
                    CloudExternalSignaling::logDiagnostic
            );
            return new CloudExternalSignaling(runtime, host, client);
        } catch (Exception | LinkageError failure) {
            try {
                if (host != null) {
                    host.transport().close().toCompletableFuture().get(STOP_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                }
            } catch (Exception cleanup) {
                failure.addSuppressed(cleanup);
            } finally {
                try {
                    store.close();
                } catch (IOException cleanup) {
                    failure.addSuppressed(cleanup);
                }
            }
            throw failure;
        }
    }

    public Channel channel() {
        return this.host.channel();
    }

    public void start() {
        this.client.start().whenComplete((registration, failure) -> {
            if (this.stopping.get()) {
                return;
            }

            if (failure != null) {
                log.warn("External signaling registration failed for {}", this.runtime.origin().getHost(), failure);
                this.client.stop();
                return;
            }

            if (registration.publicAddress() == null) {
                log.info("Registered with external signaling (instance {})", registration.instanceId());
            } else {
                log.info("Registered with external signaling at {} (instance {})", registration.publicAddress(), registration.instanceId());
            }

            this.client.operatorAction().thenAccept(action -> this.storeOperatorAction(action.orElse(null)))
                    .exceptionally(_ -> {
                        log.warn("Could not retrieve the external signaling operator link");
                        return null;
                    });
        });
    }

    @Override
    public void close() {
        if (!this.stopping.compareAndSet(false, true)) {
            return;
        }

        try {
            this.client.stop().toCompletableFuture().get(STOP_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        } catch (Exception failure) {
            log.warn("External signaling did not stop cleanly", failure);
        } finally {
            this.host.channel().close();
        }
    }

    public ChannelDuplexHandler observeRejections() {
        return new ChannelDuplexHandler() {
            @Override
            public void write(ChannelHandlerContext context, Object message, ChannelPromise promise) {
                if (message instanceof BedrockPacketWrapper wrapper && wrapper.getTargetSubClientId() == 0 && wrapper.getPacket() instanceof DisconnectPacket) {
                    CloudExternalSignaling.this.reportRejected(context.channel(), "server_rejected");
                }
                context.write(message, promise);
            }
        };
    }

    public void reportJoined(Channel channel) {
        this.reportOutcome(channel, ProviderClient.GameOutcome.JOINED, "play_ready");
    }

    public void reportRejected(Channel channel, String reason) {
        this.reportOutcome(channel, ProviderClient.GameOutcome.REJECTED, reason);
    }

    private void reportOutcome(Channel channel, ProviderClient.GameOutcome outcome, String reason) {
        AdmissionPrincipal principal = channel.attr(AdmissionPrincipal.KEY).get();
        // A disconnect after joining is not a rejected login.
        if (principal != null && channel.attr(OUTCOME_REPORTED).setIfAbsent(true) == null) {
            this.client.reportGameOutcome(principal.ticketId(), outcome, reason);
        }
    }

    public void refreshStatus() {
        this.client.requestStatusRefresh();
    }

    public static ProviderClient.BedrockProtocols protocols() {
        return new ProviderClient.BedrockProtocols(ProtocolInfo.getPacketCodecs().stream()
                .map(BedrockCodec::getProtocolVersion).distinct().sorted().toList(),
                ProtocolInfo.getDefaultMinecraftVersion());
    }

    public static ServerStatus listing(CloudServerStatus status) {
        String motd = PlainTextComponentSerializer.plainText().serialize(status.motd());
        String name = listingText(motd);
        return new ServerStatus(name.isBlank() ? "Cloudburst" : name, listingText(status.levelName()),
                Math.clamp(status.maxPlayerCount(), 0, 1_000_000),
                Math.clamp(status.gameMode().getVanillaId(), 0, 2));
    }

    private static ProviderClient.Health health(CloudServerStatus status, boolean accepting, int capacity, ProviderClient.Software software) {
        return new ProviderClient.Health(accepting, capacity, software,
                new ProviderClient.ConnectedPlayers(Math.clamp(status.playerCount(), 0, 1_000_000), System.currentTimeMillis()));
    }

    private static String listingText(String text) {
        StringBuilder result = new StringBuilder();
        text.codePoints().filter(codePoint -> Character.getType(codePoint) != Character.CONTROL
                && Character.getType(codePoint) != Character.SURROGATE).limit(128).forEach(result::appendCodePoint);
        return result.toString();
    }

    private void storeOperatorAction(@Nullable OperatorAction action) {
        if (this.stopping.get()) {
            return;
        }

        Path file = this.runtime.stateDirectory().resolve("operator-link.txt");
        try {
            Files.deleteIfExists(file);
            if (action == null) {
                return;
            }

            if (Files.getFileStore(file.getParent()).supportsFileAttributeView("posix")) {
                Files.createFile(file, PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")));
            } else {
                Files.createFile(file);
            }

            Files.writeString(file, action.text() + "\n" + action.url() + "\nExpires " + Instant.ofEpochMilli(action.expiresAt()) + "\n", StandardOpenOption.WRITE, LinkOption.NOFOLLOW_LINKS);
            log.info("The external signaling operator link is in {}", file);
        } catch (IOException failure) {
            log.warn("Could not store the external signaling operator link", failure);
        }
    }

    private static void logDiagnostic(ProviderDiagnostic diagnostic) {
        switch (diagnostic.level()) {
            case DEBUG -> log.debug("NXS: {}", diagnostic.message());
            case INFO -> log.info("NXS: {}", diagnostic.message());
            case WARN -> log.warn("NXS: {}", diagnostic.message());
        }
    }
}
