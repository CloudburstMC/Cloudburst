package org.cloudburstmc.server.network;

import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket;
import org.cloudburstmc.server.configuration.GlobalConfiguration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Limits how fast one connection may send each packet type. Packet types in the same limit share
 * one allowance that refills at a steady rate up to a burst size. Packets over the allowance are dropped.
 */
public class PacketLimiter {

    public static final PacketLimiter NONE = new PacketLimiter(Map.of());

    private static final String PACKET_PACKAGE = "org.cloudburstmc.protocol.bedrock.packet.";

    private final Map<Class<?>, Allowance> allowances;

    private PacketLimiter(Map<Class<?>, Allowance> allowances) {
        this.allowances = allowances;
    }

    /**
     * Creates a limiter with full allowances. Every packet name must name a packet type.
     */
    public static PacketLimiter create(List<GlobalConfiguration.PacketLimit> limits) {
        Map<Class<?>, Allowance> allowances = new HashMap<>();
        for (GlobalConfiguration.PacketLimit limit : limits) {
            Allowance allowance = new Allowance(limit.getRate(), limit.getBurst());
            for (String name : limit.getPackets()) {
                Class<?> type = packetType(name).orElseThrow(() -> new IllegalArgumentException("Unknown packet: " + name));
                allowances.put(type, allowance);
            }
        }

        return new PacketLimiter(Map.copyOf(allowances));
    }

    /**
     * Resolves a packet type from its simple class name, such as {@code TextPacket}.
     */
    public static Optional<Class<? extends BedrockPacket>> packetType(String name) {
        try {
            Class<?> type = Class.forName(PACKET_PACKAGE + name, false, PacketLimiter.class.getClassLoader());
            return BedrockPacket.class.isAssignableFrom(type) && !type.isInterface() ? Optional.of(type.asSubclass(BedrockPacket.class)) : Optional.empty();
        } catch (ClassNotFoundException e) {
            return Optional.empty();
        }
    }

    /**
     * Takes one packet from the allowance of the packet type.
     *
     * @return whether the packet may be handled
     */
    public boolean tryAcquire(BedrockPacket packet) {
        Allowance allowance = this.allowances.get(packet.getClass());
        return allowance == null || allowance.tryAcquire(System.nanoTime());
    }

    private static final class Allowance {

        private final double perNano;
        private final double burst;
        private double available;
        private long refilledAt;

        private Allowance(double perSecond, int burst) {
            this.perNano = perSecond / TimeUnit.SECONDS.toNanos(1);
            this.burst = burst;
            this.available = burst;
            this.refilledAt = System.nanoTime();
        }

        private synchronized boolean tryAcquire(long now) {
            this.available = Math.min(this.burst, this.available + (now - this.refilledAt) * this.perNano);
            this.refilledAt = now;
            if (this.available < 1) {
                return false;
            }

            this.available--;
            return true;
        }
    }
}
