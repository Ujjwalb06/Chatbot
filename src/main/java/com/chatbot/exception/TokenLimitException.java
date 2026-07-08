package com.chatbot.exception;

public class TokenLimitException extends RuntimeException {

    private final long minutesUntilReset;

    public TokenLimitException(long minutesUntilReset) {
        super("Token limit reached. Renews in " + minutesUntilReset + " minute(s).");
        this.minutesUntilReset = minutesUntilReset;
    }

    public long getMinutesUntilReset() {
        return minutesUntilReset;
    }
}