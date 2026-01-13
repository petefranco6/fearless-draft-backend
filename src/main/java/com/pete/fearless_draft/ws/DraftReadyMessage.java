package com.pete.fearless_draft.ws;

import com.pete.fearless_draft.DraftTurn;

public record DraftReadyMessage(
        String draftId,
        DraftTurn team,
        boolean ready
) {}
