package org.cloudburstmc.server.network.nethernet;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.netty.channel.nethernet.config.NetherServerThrottle;
import org.cloudburstmc.netty.channel.nethernet.signaling.JoinRefusal;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Limits native peer creation attempts independently of the number of active connections.
 */
public class NetherNetJoinThrottle implements NetherServerThrottle {

    private static final JoinRefusal RATE_LIMITED = new JoinRefusal(HttpResponseStatus.TOO_MANY_REQUESTS);

    private final Cache<Address, AtomicInteger> attempts = CacheBuilder.newBuilder()
            .maximumSize(4096)
            .expireAfterWrite(Duration.ofSeconds(4))
            .build();
    private final int maxAttempts;

    public NetherNetJoinThrottle(int maxAttempts) {
        if (maxAttempts <= 0) {
            throw new IllegalArgumentException("The join attempt limit must be positive");
        }

        this.maxAttempts = maxAttempts;
    }

    public @Nullable JoinRefusal refuse(@Nullable InetSocketAddress address) {
        InetAddress ip = address == null ? null : address.getAddress();
        if (ip == null) {
            return JoinRefusal.REJECTED;
        }

        AtomicInteger count = this.attempts.asMap().computeIfAbsent(Address.from(ip), ignored -> new AtomicInteger());
        return count.getAndUpdate(value -> value < this.maxAttempts ? value + 1 : value) < this.maxAttempts ? null : RATE_LIMITED;
    }

    @Override
    public boolean accept(InetSocketAddress address) {
        return this.refuse(address) == null;
    }

    @Override
    public void closed(InetSocketAddress address) {
        // Closing a connection does not reset the join attempt count.
    }

    private record Address(boolean ipv6, long prefix) {
        private static Address from(InetAddress ip) {
            byte[] bytes = ip.getAddress();
            ByteBuffer address = ByteBuffer.wrap(bytes);
            // An IPv6 host can rotate addresses within its subnet without changing its /64.
            return bytes.length == 4 ? new Address(false, address.getInt()) : new Address(true, address.getLong());
        }
    }
}
