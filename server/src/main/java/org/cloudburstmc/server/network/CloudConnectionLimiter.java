package org.cloudburstmc.server.network;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Limits simultaneous game connections globally and per IP address.
 */
public class CloudConnectionLimiter {

    private final int maxConnections;
    private final int maxConnectionsPerAddress;
    private final Map<InetAddress, Integer> connectionsPerAddress = new HashMap<>();
    private int connectionCount;

    /**
     * Creates a limiter with independent global and per-address limits.
     *
     * @param maxConnections           the maximum total number of held permits
     * @param maxConnectionsPerAddress the maximum number held by one address
     * @throws IllegalArgumentException if either limit is not positive
     */
    public CloudConnectionLimiter(int maxConnections, int maxConnectionsPerAddress) {
        if (maxConnections <= 0) {
            throw new IllegalArgumentException("maxConnections must be positive");
        }

        if (maxConnectionsPerAddress <= 0) {
            throw new IllegalArgumentException("maxConnectionsPerAddress must be positive");
        }

        this.maxConnections = maxConnections;
        this.maxConnectionsPerAddress = maxConnectionsPerAddress;
    }

    /**
     * Acquires one permit only if both quotas have room, without waiting for capacity.
     * A refused acquisition does not consume either quota.
     *
     * @param address the connection's address
     * @return whether a permit was acquired
     * @throws NullPointerException if the address is null
     */
    public synchronized boolean tryAcquire(InetAddress address) {
        Objects.requireNonNull(address, "address");
        int addressCount = this.connectionsPerAddress.getOrDefault(address, 0);
        if (this.connectionCount >= this.maxConnections || addressCount >= this.maxConnectionsPerAddress) {
            return false;
        }

        this.connectionsPerAddress.put(address, addressCount + 1);
        this.connectionCount++;
        return true;
    }

    /**
     * Returns whether the global quota is full at the time of this call.
     * This check does not reserve capacity or check the per-address limit.
     * Use {@link #tryAcquire(InetAddress)} to reserve a connection slot.
     *
     * @return whether all global permits are held
     */
    public synchronized boolean isFull() {
        return this.connectionCount >= this.maxConnections;
    }

    /**
     * Releases one permit held by the address, reclaiming both quotas.
     * An address with no held permits is ignored. Each successful acquisition must
     * be released exactly once, using an equal address.
     *
     * @param address the connection's address
     * @throws NullPointerException if the address is null
     */
    public synchronized void release(InetAddress address) {
        Objects.requireNonNull(address, "address");
        Integer addressCount = this.connectionsPerAddress.get(address);
        if (addressCount == null) {
            return;
        }

        if (addressCount == 1) {
            this.connectionsPerAddress.remove(address);
        } else {
            this.connectionsPerAddress.put(address, addressCount - 1);
        }

        this.connectionCount--;
    }
}
