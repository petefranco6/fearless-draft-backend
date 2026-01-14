package com.pete.fearless_draft;

import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class DraftEngine {

    public DraftState createInitialState(
            String draftId,
            String blueTeamName,
            String redTeamName,
            DraftTurn firstPickTeam,
            DraftMode mode,
            String seriesId,
            int gameNumber,
            List<String> lockedChampionIds
    ) {
        DraftStep[] steps = DraftOrder.build(firstPickTeam);
        DraftStep first = steps[0];

        return new DraftState(
                draftId,
                blueTeamName,
                redTeamName,
                firstPickTeam,

                first.phase(),
                0,
                first.turn(),

                List.of(),
                List.of(),
                List.of(),

                Map.of(),     // previews empty
                null,         // lastPickedChampion

                0L,           // turnStartedAt (stamped by DraftManager)
                0,            // turnDurationSeconds (stamped by DraftManager)
                0L,           // serverNow (enriched at broadcast-time)
                0L,           // turnEndsAt (enriched at broadcast-time)

                false,        // blueReady
                false,        // redReady

                mode,
                seriesId,
                gameNumber,
                lockedChampionIds == null ? List.of() : List.copyOf(lockedChampionIds)
        );
    }

    public DraftState applyAction(DraftState state, DraftAction action) {
        if (state.phase() == DraftPhase.COMPLETE) {
            throw new IllegalStateException("Draft is complete");
        }

        DraftStep[] steps = DraftOrder.build(state.firstPickTeam());

        if (state.step() < 0 || state.step() >= steps.length) {
            throw new IllegalStateException("Invalid draft step: " + state.step());
        }

        DraftStep step = steps[state.step()];

        if (step.turn() != action.team()) {
            throw new IllegalArgumentException("Not your turn");
        }

        if (step.phase() != state.phase()) {
            throw new IllegalArgumentException("Wrong phase");
        }

        boolean isNone = DraftConstants.NONE_CHAMPION_ID.equals(action.championId());

        // ✅ Fearless lock check (PICKS ONLY)
        if (!isNone && step.phase() == DraftPhase.PICK) {
            List<String> locked = state.lockedChampionIds();
            if (locked != null && locked.contains(action.championId())) {
                throw new IllegalArgumentException("Champion is locked by Fearless Draft");
            }
        }

        if (!isNone) {
            Set<String> used = new HashSet<>();
            used.addAll(state.bans());
            used.addAll(state.bluePicks());
            used.addAll(state.redPicks());

            if (used.contains(action.championId())) {
                throw new IllegalArgumentException("Champion already used");
            }
        }

        List<String> blue = new ArrayList<>(state.bluePicks());
        List<String> red = new ArrayList<>(state.redPicks());
        List<String> bans = new ArrayList<>(state.bans());

        Map<DraftTurn, String> previews = new EnumMap<>(DraftTurn.class);
        previews.putAll(state.previews());

        if (step.phase() == DraftPhase.BAN) {
            bans.add(action.championId());
        } else {
            if (action.team() == DraftTurn.BLUE) blue.add(action.championId());
            else red.add(action.championId());
        }

        String lastPickedChampion = action.championId();
        int nextStep = state.step() + 1;

        if (nextStep >= steps.length) {
            return new DraftState(
                    state.draftId(),
                    state.blueTeamName(),
                    state.redTeamName(),
                    state.firstPickTeam(),

                    DraftPhase.COMPLETE,
                    nextStep,
                    null,

                    List.copyOf(blue),
                    List.copyOf(red),
                    List.copyOf(bans),

                    previews,
                    lastPickedChampion,

                    0L,
                    0,
                    0L, // serverNow
                    0L, // turnEndsAt

                    state.blueReady(),
                    state.redReady(),

                    state.mode(),
                    state.seriesId(),
                    state.gameNumber(),
                    state.lockedChampionIds()
            );
        }

        DraftStep next = steps[nextStep];

        return new DraftState(
                state.draftId(),
                state.blueTeamName(),
                state.redTeamName(),
                state.firstPickTeam(),

                next.phase(),
                nextStep,
                next.turn(),

                List.copyOf(blue),
                List.copyOf(red),
                List.copyOf(bans),

                previews,
                lastPickedChampion,

                0L,
                0,
                0L, // serverNow
                0L, // turnEndsAt

                state.blueReady(),
                state.redReady(),

                state.mode(),
                state.seriesId(),
                state.gameNumber(),
                state.lockedChampionIds()
        );
    }
}
