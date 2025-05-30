package cn.newbeedaly.nettyim;

import cn.newbeedaly.nettyim.websocket.ChatServerHandler;
import cn.newbeedaly.nettyim.websocket.HeartBeatHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.timeout.IdleStateHandler;

import java.util.concurrent.TimeUnit;

public class ChatServer {
    private final int port;

    public ChatServer(int port) {
        this.port = port;
    }

    public void start() throws Exception {
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<Channel>() {
                        @Override
                        protected void initChannel(Channel ch) {
                            ch.pipeline()
                                    .addLast(new HttpServerCodec())
                                    .addLast(new HttpObjectAggregator(65536))
                                    .addLast(new ChunkedWriteHandler())
                                    .addLast(new WebSocketServerProtocolHandler("/ws"))
                                    // 60秒未收到客户端消息，触发空闲事件
                                    .addLast(new IdleStateHandler(5, 0, 0, TimeUnit.SECONDS))
                                    .addLast(new HeartBeatHandler())
                                    .addLast(new ChatServerHandler());
                        }
                    });

            Channel channel = bootstrap.bind(port).sync().channel();
            System.out.println("Chat server started on port: " + port);
            channel.closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}

