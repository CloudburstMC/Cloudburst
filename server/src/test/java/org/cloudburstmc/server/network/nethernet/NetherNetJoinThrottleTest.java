package org.cloudburstmc.server.network.nethernet;

import io.netty.handler.codec.http.HttpResponseStatus;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import static org.junit.jupiter.api.Assertions.*;

class NetherNetJoinThrottleTest {

    @Test
    void countsAttemptsPerIpRatherThanPerSocket() throws UnknownHostException {
        NetherNetJoinThrottle throttle = new NetherNetJoinThrottle(2);
        InetSocketAddress first = address("192.0.2.10");

        assertNull(throttle.refuse(first));
        assertNull(throttle.refuse(new InetSocketAddress(first.getAddress(), 49152)));
        assertEquals(HttpResponseStatus.TOO_MANY_REQUESTS, requireRefusal(throttle, first));
        assertNull(throttle.refuse(address("192.0.2.11")));
    }

    @Test
    void rotatingIpv6HostBitsDoesNotResetTheAttemptLimit() throws UnknownHostException {
        NetherNetJoinThrottle throttle = new NetherNetJoinThrottle(1);

        assertNull(throttle.refuse(address("2001:db8:1:2::1")));
        assertEquals(HttpResponseStatus.TOO_MANY_REQUESTS, requireRefusal(throttle, address("2001:db8:1:2::2")));
        assertNull(throttle.refuse(address("2001:db8:1:3::1")));
    }

    private static HttpResponseStatus requireRefusal(NetherNetJoinThrottle throttle, InetSocketAddress address) {
        return java.util.Objects.requireNonNull(throttle.refuse(address)).status();
    }

    private static InetSocketAddress address(String host) throws UnknownHostException {
        return new InetSocketAddress(InetAddress.getByName(host), 19132);
    }
}
