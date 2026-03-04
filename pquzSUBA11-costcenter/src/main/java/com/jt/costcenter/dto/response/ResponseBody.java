package com.jt.costcenter.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class ResponseBody {

    @JsonProperty("messages")
    private List<Message> messages;

    @JsonProperty("value")
    private Value value;

    @JsonProperty("isSuccess")
    private boolean isSuccess;

    public ResponseBody() {
    }

    public ResponseBody(List<Message> messages, Value value, boolean isSuccess) {
        this.messages = messages;
        this.value = value;
        this.isSuccess = isSuccess;
    }

    public List<Message> getMessages() {
        return messages;
    }

    public void setMessages(List<Message> messages) {
        this.messages = messages;
    }

    public Value getValue() {
        return value;
    }

    public void setValue(Value value) {
        this.value = value;
    }

    public boolean getIsSuccess() {
        return isSuccess;
    }

    public void setIsSuccess(boolean isSuccess) {
        this.isSuccess = isSuccess;
    }
}