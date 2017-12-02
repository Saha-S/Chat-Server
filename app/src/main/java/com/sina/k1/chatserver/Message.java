package com.sina.k1.chatserver;

public class Message {

    public static final int TYPE_LEFT = 0;
    public static final int TYPE_LOG = 1;
    public static final int TYPE_ACTION = 2;
    public static final int TYPE_VOICE = 3;
    public static final int TYPE_RIGHT = 4;

    private int mType;
    private String mMessage;
    private String mUsername;

    private Message() {}

    public int getType() {
        return mType;
    };

    public String getMessage() {
        return mMessage;
    };

    public String getUsername() {
        return mUsername;
    };


    public static class Builder {
        private final int mType;
        private String mUsername;
        private String mMessage;

        public Builder(int type) {
            mType = type;
        }

        public Builder username(String username) {
            mUsername = username;
            return this;
        }

        public Builder message(String message) {
            mMessage = message;
            return this;
        }

        public Message build() {
            Message message = new Message();
            message.mType = mType;
            message.mUsername = mUsername;
            message.mMessage = mMessage;
            return message;
        }
    }
}
