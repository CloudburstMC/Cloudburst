package org.cloudburstmc.server.network.nethernet;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class CloudNetherNetExceptionHandler extends ChannelInboundHandlerAdapter {

    public static final String NAME = "cloud-nethernet-exception-handler";

    @Override
    public void exceptionCaught(ChannelHandlerContext context, Throwable cause) {
        if (context.channel().isOpen()) {
            log.debug("Closing NetherNet connection {} after {}", context.channel().remoteAddress(), cause.getClass().getSimpleName());
        }

        context.close();
    }
}
