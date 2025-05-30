package cn.newbeedaly.nettyim;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;

public class ChatMsg {

    public ChatMsg() {
    }

    public ChatMsg(String userId, String msgType, String targetUserId, String message) {
        this.userId = userId;
        this.msgType = msgType;
        this.targetUserId = targetUserId;
        this.message = message;
    }

    @JsonSetter(nulls = Nulls.SKIP)
    private String userId;
    @JsonSetter(nulls = Nulls.SKIP)
    private String msgType;
    @JsonSetter(nulls = Nulls.SKIP)
    private String targetUserId;
    @JsonSetter(nulls = Nulls.SKIP)
    private String message;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getMsgType() {
        return msgType;
    }

    public void setMsgType(String msgType) {
        this.msgType = msgType;
    }

    public String getTargetUserId() {
        return targetUserId;
    }

    public void setTargetUserId(String targetUserId) {
        this.targetUserId = targetUserId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
