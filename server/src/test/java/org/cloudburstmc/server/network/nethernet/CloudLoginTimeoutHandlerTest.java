package org.cloudburstmc.server.network.nethernet;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class CloudLoginTimeoutHandlerTest {

    @Test
    void closesAtTheAbsoluteDeadlineEvenWhenJunkKeepsArriving() throws Exception {
        LifecycleObserver observer = new LifecycleObserver();
        EmbeddedChannel channel = new EmbeddedChannel(false, false, new CloudLoginTimeoutHandler(30), observer);
        channel.freezeTime();

        try {
            channel.register();
            assertEquals(1, observer.activeEvents);

            for (int second = 1; second < 30; second++) {
                channel.advanceTimeBy(1, TimeUnit.SECONDS);
                ByteBuf junk = Unpooled.buffer().writeByte(second);

                assertTrue(channel.writeInbound(junk));

                ByteBuf forwarded = channel.readInbound();

                try {
                    assertSame(junk, forwarded);
                } finally {
                    forwarded.release();
                }

                channel.runScheduledPendingTasks();
                assertTrue(channel.isActive());
            }

            channel.advanceTimeBy(1, TimeUnit.SECONDS);
            channel.runScheduledPendingTasks();
            channel.runPendingTasks();
            channel.checkException();

            assertFalse(channel.isOpen());
            assertEquals(1, observer.inactiveEvents);
        } finally {
            channel.finishAndReleaseAll();
        }
    }

    @Test
    void startsOnlyOnChannelActiveAndUsesTheConfiguredSeconds() throws Exception {
        EmbeddedChannel channel = new EmbeddedChannel(false, false, new CloudLoginTimeoutHandler(7));
        channel.freezeTime();

        try {
            channel.advanceTimeBy(1, TimeUnit.MINUTES);
            assertEquals(-1L, channel.runScheduledPendingTasks());
            assertTrue(channel.isOpen());

            channel.register();
            channel.advanceTimeBy(6999, TimeUnit.MILLISECONDS);
            channel.runScheduledPendingTasks();
            assertTrue(channel.isActive());

            channel.advanceTimeBy(1, TimeUnit.MILLISECONDS);
            channel.runScheduledPendingTasks();
            channel.runPendingTasks();
            channel.checkException();
            assertFalse(channel.isOpen());
        } finally {
            channel.finishAndReleaseAll();
        }
    }

    @Test
    void removingTheHandlerCancelsTheDeadline() throws Exception {
        CloudLoginTimeoutHandler timeout = new CloudLoginTimeoutHandler(30);
        LifecycleObserver observer = new LifecycleObserver();
        EmbeddedChannel channel = new EmbeddedChannel(false, false, timeout, observer);
        channel.freezeTime();

        try {
            channel.register();
            channel.advanceTimeBy(29, TimeUnit.SECONDS);
            channel.pipeline().remove(timeout);

            channel.advanceTimeBy(1, TimeUnit.MINUTES);
            assertEquals(-1L, channel.runScheduledPendingTasks());
            channel.checkException();
            assertTrue(channel.isActive());
            assertEquals(1, observer.activeEvents);
            assertEquals(0, observer.inactiveEvents);
        } finally {
            channel.finishAndReleaseAll();
        }
    }

    @Test
    void channelInactiveCancelsTheDeadlineAndPropagatesTheEvent() throws Exception {
        LifecycleObserver observer = new LifecycleObserver();
        EmbeddedChannel channel = new EmbeddedChannel(false, false, new CloudLoginTimeoutHandler(30), observer);
        channel.freezeTime();

        try {
            channel.register();
            channel.advanceTimeBy(5, TimeUnit.SECONDS);

            channel.pipeline().fireChannelInactive();
            assertEquals(1, observer.inactiveEvents);

            channel.advanceTimeBy(1, TimeUnit.MINUTES);
            assertEquals(-1L, channel.runScheduledPendingTasks());
            channel.checkException();
            assertTrue(channel.isOpen());
        } finally {
            channel.finishAndReleaseAll();
        }
    }

    private static class LifecycleObserver extends ChannelInboundHandlerAdapter {

        private int activeEvents;
        private int inactiveEvents;

        @Override
        public void channelActive(ChannelHandlerContext context) {
            this.activeEvents++;
            context.fireChannelActive();
        }

        @Override
        public void channelInactive(ChannelHandlerContext context) {
            this.inactiveEvents++;
            context.fireChannelInactive();
        }
    }
}
