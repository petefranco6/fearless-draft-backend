package com.pete.fearless_draft;

import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
public class DraftService {

    private final DraftEngine engine;

    public DraftService(DraftEngine engine) {
        this.engine = engine;
    }

    // ✅ Single-game create
    public DraftState createDraft(String draftId, String blueTeamName, String redTeamName, DraftTurn firstPickTeam) {
        return engine.createInitialState(
                draftId,
                blueTeamName,
                redTeamName,
                firstPickTeam,
                DraftMode.SINGLE,
                null,
                1,
                List.of()
        );
    }

    // ✅ Fearless series create
    public DraftState createFearlessDraft(
            String draftId,
            String blueTeamName,
            String redTeamName,
            DraftTurn firstPickTeam,
            String seriesId,
            int gameNumber,
            List<String> lockedChampionIds
    ) {
        return engine.createInitialState(
                draftId,
                blueTeamName,
                redTeamName,
                firstPickTeam,
                DraftMode.FEARLESS_SERIES,
                seriesId,
                gameNumber,
                lockedChampionIds
        );
    }

    public DraftState setPreview(DraftState state, DraftTurn team, String championId) {
        Map<DraftTurn, String> newPreviews = new EnumMap<>(DraftTurn.class);
        newPreviews.putAll(state.previews());
        newPreviews.put(team, championId);

        return new DraftState(
                state.draftId(),
                state.blueTeamName(),
                state.redTeamName(),
                state.firstPickTeam(),
                state.phase(),
                state.step(),
                state.turn(),
                state.bluePicks(),
                state.redPicks(),
                state.bans(),
                newPreviews,
                state.lastPickedChampion(),
                state.turnStartedAt(),
                state.turnDurationSeconds(),
                state.blueReady(),
                state.redReady(),
                state.mode(),
                state.seriesId(),
                state.gameNumber(),
                state.lockedChampionIds()
        );
    }

    public DraftState applyAction(DraftState state, DraftAction action) {
        DraftState next = engine.applyAction(state, action);

        Map<DraftTurn, String> previews = new EnumMap<>(DraftTurn.class);
        previews.putAll(next.previews());
        previews.remove(action.team());

        return new DraftState(
                next.draftId(),
                state.blueTeamName(),
                state.redTeamName(),
                state.firstPickTeam(),
                next.phase(),
                next.step(),
                next.turn(),
                next.bluePicks(),
                next.redPicks(),
                next.bans(),
                previews,
                next.lastPickedChampion(),
                next.turnStartedAt(),
                next.turnDurationSeconds(),
                next.blueReady(),
                next.redReady(),
                next.mode(),
                next.seriesId(),
                next.gameNumber(),
                next.lockedChampionIds()
        );
    }
}
