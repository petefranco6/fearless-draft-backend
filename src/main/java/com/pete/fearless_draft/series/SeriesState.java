package com.pete.fearless_draft.series;

import com.pete.fearless_draft.DraftTurn;

import java.util.Set;

public record SeriesState(
        String seriesId,
        String blueTeamName,
        String redTeamName,
        DraftTurn firstPickTeam,
        int bestOf,
        int currentGame,
        String currentDraftId,
        Set<String> lockedChampionIds
) {}
