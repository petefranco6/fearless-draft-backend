package com.pete.fearless_draft;

public record DraftPreview(
        String draftId,
        DraftTurn team,
        String championId
) {}
