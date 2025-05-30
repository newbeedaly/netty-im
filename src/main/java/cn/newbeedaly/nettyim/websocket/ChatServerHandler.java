package cn.newbeedaly.nettyim.websocket;

import cn.newbeedaly.nettyim.ChatMsg;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.util.concurrent.GlobalEventExecutor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ChatServerHandler extends SimpleChannelInboundHandler<WebSocketFrame> {
    // Store connected clients
    private static final ChannelGroup channelGroup = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
    // Store connected clients (userId -> Channel)
    private static final Map<String, Channel> clients = new ConcurrentHashMap<>();
    // Store offline messages (userId -> List of messages)
    private static final Map<String, List<String>> offlineMessages = new ConcurrentHashMap<>();

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        channelGroup.add(ctx.channel());
        System.out.println("Client connected: " + ctx.channel().id() + " " + ctx.channel());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        channelGroup.remove(ctx.channel());
        String userId = getUserIdByChannel(ctx.channel());
        if (userId != null) {
            clients.remove(userId);
            System.out.println("Client disconnected: " + userId);
        }
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, WebSocketFrame frame) {
        if (frame instanceof TextWebSocketFrame) {
            String text = ((TextWebSocketFrame) frame).text();
            System.out.println("Received: " + text);

            ChatMsg chatMsg;
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false); // 忽略 JSON 中多余字段（不报错）
                chatMsg = objectMapper.readValue(text, ChatMsg.class);
                String userId = chatMsg.getUserId();
                String msgType = chatMsg.getMsgType();
                String targetUserId = chatMsg.getTargetUserId();
                String message = chatMsg.getMessage();
                // 消息类型 0 INIT 1 TEXT
                if (msgType.equals("0")) {
                    // Register user
                    clients.put(userId, ctx.channel());

                    // Send any pending offline messages to the newly connected user
                    if (offlineMessages.containsKey(userId)) {
                        List<String> pendingMessages = offlineMessages.get(userId);
                        for (String pendingMessage : pendingMessages) {
                            ctx.channel().writeAndFlush(new TextWebSocketFrame(pendingMessage));
                        }
                        offlineMessages.remove(userId); // Clear delivered messages
                        System.out.println("Delivered " + pendingMessages.size() + " offline messages to " + userId);
                    }
                    return;
                }

                // Send message to target user
                Channel targetChannel = clients.get(targetUserId);
                String formattedMessage = userId + ": " + message;
                if (targetChannel != null && targetChannel.isActive()) {
                    targetChannel.writeAndFlush(new TextWebSocketFrame(formattedMessage));
                    //ctx.channel().writeAndFlush(new TextWebSocketFrame("Message sent to " + targetUserId));
                    ctx.channel().writeAndFlush(new TextWebSocketFrame(formattedMessage));
                } else {
                    // Store message for offline user
                    offlineMessages.computeIfAbsent(targetUserId, k -> new ArrayList<>())
                            .add(formattedMessage);
                    ctx.channel().writeAndFlush(new TextWebSocketFrame("Target user " + targetUserId + " is offline. Message stored."));
                    System.out.println("Stored message for offline user " + targetUserId + ": " + formattedMessage);
                }
            } catch (JsonProcessingException e) {
                ctx.channel().writeAndFlush(new TextWebSocketFrame("Invalid message format. Use: userId:targetUserId:message"));
                throw new RuntimeException(e);
            }

        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        channelGroup.remove(ctx.channel());
        String userId = getUserIdByChannel(ctx.channel());
        if (userId != null) {
            clients.remove(userId);
            System.out.println("Client disconnected: " + userId);
        }
        ctx.close();
    }

    private String getUserIdByChannel(Channel channel) {
        for (Map.Entry<String, Channel> entry : clients.entrySet()) {
            if (entry.getValue() == channel) {
                return entry.getKey();
            }
        }
        return null;
    }
}