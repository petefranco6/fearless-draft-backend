package com.pete.fearless_draft;

public record DraftAction(
        String draftId,
        DraftTurn team,
        String championId
) {
}
