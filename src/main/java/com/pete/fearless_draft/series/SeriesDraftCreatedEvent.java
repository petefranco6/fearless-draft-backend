package com.pete.fearless_draft.series;

public record SeriesDraftCreatedEvent(
        String type,              // "SERIES_DRAFT_CREATED"
        String seriesId,
        int gameNumber,
        String draftId,
        String createdBy          // optional: "BLUE"/"RED"/"UNKNOWN" or a clientId
) {}
