package org.cloudburstmc.server.network;

import io.netty.buffer.Unpooled;
import org.cloudburstmc.protocol.bedrock.packet.UnknownPacket;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class GameplayPacketQueueTest {

    @Test
    void releasesPacketsAfterDispatchAndOnClose() {
        UnknownPacket delivered = packet();
        UnknownPacket discarded = packet();
        try (GameplayPacketQueue queue = new GameplayPacketQueue()) {
            assertTrue(queue.offer(delivered, 1));
            delivered.release();
            queue.drain(packet -> assertEquals(42, assertInstanceOf(UnknownPacket.class, packet).getPayload().readByte()));

            assertEquals(0, delivered.refCnt());
            assertTrue(queue.offer(discarded, 1));
            discarded.release();
        }

        assertEquals(0, discarded.refCnt());
    }

    @Test
    void boundsTheQueueAndDrainsOnlyOneTickOfWork() {
        UnknownPacket packet = packet();
        AtomicInteger delivered = new AtomicInteger();
        try (GameplayPacketQueue queue = new GameplayPacketQueue()) {
            for (int i = 0; i < 256; i++) {
                assertTrue(queue.offer(packet, 1));
            }
            assertFalse(queue.offer(packet, 1));
            assertEquals(257, packet.refCnt());

            queue.drain(ignored -> delivered.incrementAndGet());
            assertEquals(64, delivered.get());
            assertEquals(193, packet.refCnt());
        } finally {
            assertEquals(1, packet.refCnt());
            packet.release();
        }
    }

    @Test
    void limitsBytesAndStillMakesProgressOnALargeHeadPacket() {
        UnknownPacket packet = packet();
        AtomicInteger delivered = new AtomicInteger();
        try (GameplayPacketQueue queue = new GameplayPacketQueue()) {
            assertTrue(queue.offer(packet, 1024 * 1024));
            assertTrue(queue.offer(packet, 1024 * 1024));
            assertFalse(queue.offer(packet, 1));

            queue.drain(ignored -> delivered.incrementAndGet());
            assertEquals(1, delivered.get());
            queue.drain(ignored -> delivered.incrementAndGet());
            assertEquals(2, delivered.get());
        } finally {
            packet.release();
        }
    }

    private static UnknownPacket packet() {
        UnknownPacket packet = new UnknownPacket();
        packet.setPayload(Unpooled.buffer(1).writeByte(42));
        return packet;
    }
}
