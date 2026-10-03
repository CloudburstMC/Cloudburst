package org.cloudburstmc.server.network;

import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

class CloudConnectionLimiterTest {

    @Test
    void requiresPositiveLimitsAndNonNullAddresses() {
        assertThrows(IllegalArgumentException.class, () -> new CloudConnectionLimiter(0, 1));
        assertThrows(IllegalArgumentException.class, () -> new CloudConnectionLimiter(-1, 1));
        assertThrows(IllegalArgumentException.class, () -> new CloudConnectionLimiter(1, 0));
        assertThrows(IllegalArgumentException.class, () -> new CloudConnectionLimiter(1, -1));

        CloudConnectionLimiter limiter = new CloudConnectionLimiter(1, 1);

        assertThrows(NullPointerException.class, () -> limiter.tryAcquire(null));
        assertThrows(NullPointerException.class, () -> limiter.release(null));
    }

    @Test
    void enforcesBothBoundariesWithoutChargingRefusedAcquisitions() throws Exception {
        CloudConnectionLimiter limiter = new CloudConnectionLimiter(3, 2);
        InetAddress first = address(1);
        InetAddress second = address(2);
        InetAddress third = address(3);

        assertTrue(limiter.tryAcquire(first));
        assertTrue(limiter.tryAcquire(first));
        assertFalse(limiter.tryAcquire(first));
        assertTrue(limiter.tryAcquire(second));
        assertFalse(limiter.tryAcquire(second));
        assertFalse(limiter.tryAcquire(third));

        limiter.release(first);

        assertTrue(limiter.tryAcquire(second));
        assertFalse(limiter.tryAcquire(third));

        limiter.release(second);

        assertTrue(limiter.tryAcquire(first));
        assertFalse(limiter.tryAcquire(first));
    }

    @Test
    void reportsOnlyGlobalFullnessAndRechecksAfterPermitRelease() throws Exception {
        CloudConnectionLimiter limiter = new CloudConnectionLimiter(2, 1);
        InetAddress first = address(1);
        InetAddress second = address(2);

        assertFalse(limiter.isFull());

        assertTrue(limiter.tryAcquire(first));
        assertFalse(limiter.tryAcquire(first));
        assertFalse(limiter.isFull());

        assertTrue(limiter.tryAcquire(second));
        assertTrue(limiter.isFull());
        assertFalse(limiter.tryAcquire(address(3)));

        limiter.release(address(3));

        assertTrue(limiter.isFull());

        limiter.release(first);
        assertFalse(limiter.isFull());
        assertTrue(limiter.tryAcquire(first));
        assertTrue(limiter.isFull());

        limiter.release(first);
        limiter.release(second);

        assertFalse(limiter.isFull());
    }

    @Test
    void reclaimsPermitsUsingEqualAddressesAndIgnoresUnheldAddresses() throws Exception {
        CloudConnectionLimiter limiter = new CloudConnectionLimiter(1, 3);
        InetAddress first = address(1);

        assertTrue(limiter.tryAcquire(first));

        limiter.release(address(2));

        assertFalse(limiter.tryAcquire(address(2)));

        limiter.release(address(1));
        limiter.release(first);

        assertTrue(limiter.tryAcquire(address(2)));
        assertFalse(limiter.tryAcquire(first));

        limiter.release(address(2));

        assertTrue(limiter.tryAcquire(first));
    }

    @Test
    void concurrentAcquisitionsCannotExceedThePerAddressLimit() throws Exception {
        CloudConnectionLimiter limiter = new CloudConnectionLimiter(32, 5);
        InetAddress shared = address(1);
        List<Callable<Boolean>> attempts = new ArrayList<>();

        for (int i = 0; i < 32; i++) {
            attempts.add(() -> limiter.tryAcquire(shared));
        }

        List<Boolean> acquired = concurrently(attempts);

        assertEquals(5, Collections.frequency(acquired, true));
        assertTrue(limiter.tryAcquire(address(2)));

        for (boolean success : acquired) {
            if (success) {
                limiter.release(shared);
            }
        }

        for (int i = 0; i < 5; i++) {
            assertTrue(limiter.tryAcquire(shared));
        }

        assertFalse(limiter.tryAcquire(shared));
    }

    @Test
    void concurrentAcquisitionsCannotExceedTheGlobalLimit() throws Exception {
        CloudConnectionLimiter limiter = new CloudConnectionLimiter(7, 1);
        List<Callable<Boolean>> attempts = new ArrayList<>();

        for (int i = 1; i <= 32; i++) {
            InetAddress candidate = address(i);
            attempts.add(() -> limiter.tryAcquire(candidate));
        }

        List<Boolean> acquired = concurrently(attempts);

        assertEquals(7, Collections.frequency(acquired, true));

        for (int i = 0; i < acquired.size(); i++) {
            if (acquired.get(i)) {
                limiter.release(address(i + 1));
            }
        }

        for (int i = 1; i <= 7; i++) {
            assertTrue(limiter.tryAcquire(address(i)));
        }

        assertFalse(limiter.tryAcquire(address(8)));
    }

    @Test
    void concurrentAcquireReleaseCyclesLeaveAllPermitsReclaimable() throws Exception {
        CloudConnectionLimiter limiter = new CloudConnectionLimiter(7, 2);
        List<Callable<Integer>> workers = new ArrayList<>();

        for (int i = 0; i < 24; i++) {
            InetAddress candidate = address(i % 4 + 1);

            workers.add(() -> {
                int acquisitions = 0;

                for (int attempt = 0; attempt < 1000; attempt++) {
                    if (limiter.tryAcquire(candidate)) {
                        try {
                            acquisitions++;
                            Thread.yield();
                        } finally {
                            limiter.release(candidate);
                        }
                    }
                }

                return acquisitions;
            });
        }

        assertTrue(concurrently(workers).stream().mapToInt(Integer::intValue).sum() > 0);

        for (int i = 1; i <= 3; i++) {
            assertTrue(limiter.tryAcquire(address(i)));
            assertTrue(limiter.tryAcquire(address(i)));
            assertFalse(limiter.tryAcquire(address(i)));
        }

        assertTrue(limiter.tryAcquire(address(4)));
        assertFalse(limiter.tryAcquire(address(4)));

        limiter.release(address(1));

        assertTrue(limiter.tryAcquire(address(4)));
        assertFalse(limiter.tryAcquire(address(4)));
    }

    private static InetAddress address(int suffix) throws UnknownHostException {
        return InetAddress.getByAddress(new byte[]{(byte) 192, 0, 2, (byte) suffix});
    }

    private static <T> List<T> concurrently(List<Callable<T>> tasks) throws Exception {
        CountDownLatch ready = new CountDownLatch(tasks.size());
        CountDownLatch start = new CountDownLatch(1);

        try (ExecutorService executor = Executors.newFixedThreadPool(tasks.size())) {
            try {
                List<Future<T>> futures = new ArrayList<>();

                for (Callable<T> task : tasks) {
                    futures.add(executor.submit(() -> {
                        ready.countDown();

                        if (!start.await(10, TimeUnit.SECONDS)) {
                            throw new IllegalStateException("Concurrent start timed out");
                        }

                        return task.call();
                    }));
                }

                assertTrue(ready.await(10, TimeUnit.SECONDS));
                start.countDown();

                List<T> results = new ArrayList<>();

                for (Future<T> future : futures) {
                    results.add(future.get(10, TimeUnit.SECONDS));
                }

                return results;
            } finally {
                start.countDown();
                executor.shutdownNow();
                assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
            }
        }
    }
}
