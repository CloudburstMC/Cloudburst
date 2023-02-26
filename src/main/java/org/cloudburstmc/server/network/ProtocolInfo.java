package org.cloudburstmc.server.network;

import lombok.extern.log4j.Log4j2;
import org.cloudburstmc.protocol.bedrock.codec.BedrockCodec;
import org.cloudburstmc.protocol.bedrock.codec.v568.Bedrock_v568;

import javax.annotation.Nonnegative;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.google.common.base.Preconditions.checkNotNull;

@Log4j2
@ParametersAreNonnullByDefault
public final class ProtocolInfo {
    private static final Set<BedrockCodec> PACKET_CODECS = ConcurrentHashMap.newKeySet();
    private static final Set<BedrockCodec> UNMODIFIABLE_PACKET_CODECS = Collections.unmodifiableSet(PACKET_CODECS);

    private static BedrockCodec DEFAULT_PACKET_CODEC;

    static {
        setDefaultPacketCodec(Bedrock_v568.CODEC);
    }

    public static BedrockCodec getDefaultPacketCodec() {
        return DEFAULT_PACKET_CODEC;
    }

    public static void setDefaultPacketCodec(BedrockCodec packetCodec) {
        DEFAULT_PACKET_CODEC = checkNotNull(packetCodec, "packetCodec");
        PACKET_CODECS.add(DEFAULT_PACKET_CODEC);
    }

    public static String getDefaultMinecraftVersion() {
        return DEFAULT_PACKET_CODEC.getMinecraftVersion();
    }

    public static int getDefaultProtocolVersion() {
        return DEFAULT_PACKET_CODEC.getProtocolVersion();
    }

    @Nullable
    public static BedrockCodec getPacketCodec(@Nonnegative int protocolVersion) {
        for (BedrockCodec packetCodec : PACKET_CODECS) {
            if (packetCodec.getProtocolVersion() == protocolVersion) {
                return packetCodec;
            }
        }
        return null;
    }

    public static void addPacketCodec(BedrockCodec packetCodec) {
        PACKET_CODECS.add(checkNotNull(packetCodec, "packetCodec"));
    }

    public static Set<BedrockCodec> getPacketCodecs() {
        return UNMODIFIABLE_PACKET_CODECS;
    }
}
