package com.grid07.coreapi.service;

public enum InteractionType {
    BOT_REPLY(1),
    HUMAN_LIKE(20),
    HUMAN_COMMENT(50);

    private final int scoreDelta;

    InteractionType(int scoreDelta) {
        this.scoreDelta = scoreDelta;
    }

    public int scoreDelta() {
        return scoreDelta;
    }
}
