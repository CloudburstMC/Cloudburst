package org.cloudburstmc.server.network.nethernet;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.concurrent.ScheduledFuture;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import java.util.concurrent.TimeUnit;

/**
 * Applies an absolute deadline through client readiness, unaffected by incoming traffic.
 */
@RequiredArgsConstructor(access = AccessLevel.PUBLIC)
public class CloudLoginTimeoutHandler extends ChannelInboundHandlerAdapter {

    private final int timeoutSeconds;
    private ScheduledFuture<?> timeout;

    @Override
    @SuppressWarnings("resource")
    public void channelActive(ChannelHandlerContext context) {
        if (this.timeout == null) {
            this.timeout = context.executor().schedule(() -> context.channel().close(), this.timeoutSeconds, TimeUnit.SECONDS);
        }

        context.fireChannelActive();
    }

    @Override
    public void channelInactive(ChannelHandlerContext context) {
        this.cancelTimeout();
        context.fireChannelInactive();
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext context) throws Exception {
        this.cancelTimeout();
        super.handlerRemoved(context);
    }

    private void cancelTimeout() {
        if (this.timeout != null) {
            this.timeout.cancel(false);
            this.timeout = null;
        }
    }
}
