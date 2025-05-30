package cn.newbeedaly.nettyim.config;

import cn.newbeedaly.nettyim.ChatServer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class NettyConfig {

    @Value("${netty.port:8081}")
    private int port;

    @Bean
    public ChatServer chatServer() {
        ChatServer server = new ChatServer(port);
        new Thread(() -> {
            try {
                server.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
        return server;
    }
}