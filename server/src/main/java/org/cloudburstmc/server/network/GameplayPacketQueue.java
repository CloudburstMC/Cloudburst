package org.cloudburstmc.server.network;

import io.netty.util.ReferenceCountUtil;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket;

import java.util.ArrayDeque;
import java.util.function.Consumer;

/**
 * Bounds gameplay input waiting for the server thread.
 */
public class GameplayPacketQueue implements AutoCloseable {

    private static final int MAX_PACKETS = 256;
    private static final int MAX_BYTES = 2 * 1024 * 1024;
    private static final int PACKETS_PER_TICK = 64;
    private static final int BYTES_PER_TICK = 256 * 1024;

    private final ArrayDeque<Entry> pending = new ArrayDeque<>();
    private int bytes;
    private boolean closed;

    /**
     * Retains an accepted packet. The caller keeps responsibility for its original reference.
     */
    public synchronized boolean offer(BedrockPacket packet, int size) {
        if (this.closed || size <= 0 || size > MAX_BYTES - this.bytes || this.pending.size() >= MAX_PACKETS) {
            return false;
        }

        this.pending.addLast(new Entry(ReferenceCountUtil.retain(packet), size));
        this.bytes += size;
        return true;
    }

    /**
     * Dispatches one tick's input in arrival order. The handler borrows each packet for the call.
     */
    public void drain(Consumer<BedrockPacket> handler) {
        int budget = BYTES_PER_TICK;
        for (int i = 0; i < PACKETS_PER_TICK && budget > 0; i++) {
            // An accepted packet larger than the tick budget must still be able to make progress.
            Entry entry = this.poll(i == 0 ? MAX_BYTES : budget);
            if (entry == null) {
                break;
            }

            budget -= entry.size();
            try {
                handler.accept(entry.packet());
            } finally {
                ReferenceCountUtil.release(entry.packet());
            }
        }
    }

    private synchronized Entry poll(int budget) {
        Entry entry = this.pending.peekFirst();
        if (entry == null || entry.size() > budget) {
            return null;
        }

        this.pending.removeFirst();
        this.bytes -= entry.size();
        return entry;
    }

    /**
     * Discards pending packets and rejects new ones.
     */
    @Override
    public synchronized void close() {
        this.closed = true;
        Entry entry;
        while ((entry = this.pending.pollFirst()) != null) {
            ReferenceCountUtil.release(entry.packet());
        }
        this.bytes = 0;
    }

    private record Entry(BedrockPacket packet, int size) {
    }
}
