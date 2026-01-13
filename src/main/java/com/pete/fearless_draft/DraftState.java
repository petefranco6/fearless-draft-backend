package com.pete.fearless_draft;

import java.util.List;
import java.util.Map;

public record DraftState(
        String draftId,
        String blueTeamName,
        String redTeamName,
        DraftTurn firstPickTeam,
        DraftPhase phase,
        int step,
        DraftTurn turn,
        List<String> bluePicks,
        List<String> redPicks,
        List<String> bans,
        Map<DraftTurn, String> previews,
        String lastPickedChampion,
        long turnStartedAt,
        int turnDurationSeconds,

        boolean blueReady,
        boolean redReady,

        DraftMode mode,
        String seriesId,
        int gameNumber,
        List<String> lockedChampionIds
) {}
