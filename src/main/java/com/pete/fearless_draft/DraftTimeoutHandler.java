package com.pete.fearless_draft;

public interface DraftTimeoutHandler {
    void onTurnTimeout(String draftId, DraftPhase expectedPhase, int expectedStep, long expectedStartedAt);
}
