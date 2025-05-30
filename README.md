# netty-im

单节点netty即时聊天项目实践

**集群改造考虑点**

1. 会话共享：用户重连可能连接到不同节点

```java
// 原单体版会话存储
private static final Map<String, Channel> clients = new ConcurrentHashMap<>();

// 集群版改为
private final RedisTemplate<String, String> redisTemplate;

public void saveSession(String userId, String nodeIp) {
    redisTemplate.opsForValue().set("user_session:" + userId, nodeIp);
}
```
2. 跨节点消息传递（消息路由）：用户A在节点1，用户B在节点2，如何传递消息

架构图

客户端 → 负载均衡器 → [Netty节点1, Netty节点2, Netty节点3]

                        ↓
                      [Redis集群] - 存储会话/状态
                      [Kafka集群] - 处理消息路由
                      [MySQL集群] - 持久化存储
              
```java
// 原单体版直接发送
channel.writeAndFlush(message);

// 集群版改为
public void sendMessage(Message msg) {
    String targetNode = redisTemplate.opsForValue().get("user_session:" + msg.getTo());
    if(targetNode.equals(localNodeIp)) {
        // 本地直接发送
        localChannel.writeAndFlush(message);
    } else {
        // 跨节点通过MQ发送
        kafkaTemplate.send("node_" + targetNode, message);
    }
}

// 新增节点间消息监听（节点间通信）
@KafkaListener(topics = "node_" + "${node.ip}")
public void listenNodeMessage(Message msg) {
    Channel channel = localSessionStore.get(msg.getTo());
    if(channel != null) {
        channel.writeAndFlush(msg);
    }
}
```
4. 消息顺序保证：聊天时间
5. 消息持久化
