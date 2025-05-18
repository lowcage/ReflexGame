package com.example.reflexgame.models;

import com.google.firebase.Timestamp;

public class Result {
    private String userEmail;
    private long reactionTimeMs;
    private Timestamp timestamp;

    public Result() {
        // Firestore-nek kell az üres konstruktor
    }

    public Result(String userEmail, long reactionTimeMs, Timestamp timestamp) {
        this.userEmail = userEmail;
        this.reactionTimeMs = reactionTimeMs;
        this.timestamp = timestamp;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public long getReactionTimeMs() {
        return reactionTimeMs;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public void setReactionTimeMs(long reactionTimeMs) {
        this.reactionTimeMs = reactionTimeMs;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }
}
