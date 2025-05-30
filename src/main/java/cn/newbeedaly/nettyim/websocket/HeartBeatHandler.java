package cn.newbeedaly.nettyim.websocket;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleStateEvent;

public class HeartBeatHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
        if (evt instanceof IdleStateEvent event) {
            switch (event.state()) {
                case READER_IDLE -> System.out.println("进入读空闲状态");
                case WRITER_IDLE -> System.out.println("进入写空闲状态");
                default -> {
                    System.out.println("触发断线");
                    Channel channel = ctx.channel();
                    channel.close();
                }
            }
        }
    }
}