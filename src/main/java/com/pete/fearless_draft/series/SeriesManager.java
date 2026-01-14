package com.pete.fearless_draft.series;

import com.pete.fearless_draft.*;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SeriesManager {

    private final DraftService draftService;
    private final DraftManager draftManager;
    private final SimpMessagingTemplate brokerMessagingTemplate;

    private final Map<String, SeriesState> seriesMap = new ConcurrentHashMap<>();

    public SeriesManager(
            DraftService draftService,
            DraftManager draftManager,
            SimpMessagingTemplate brokerMessagingTemplate
    ) {
        this.draftService = draftService;
        this.draftManager = draftManager;
        this.brokerMessagingTemplate = brokerMessagingTemplate;
    }

    public DraftState createSeries(CreateSeriesRequest req) {
        if (req.getBestOf() != 3 && req.getBestOf() != 5) {
            throw new IllegalArgumentException("bestOf must be 3 or 5");
        }

        String seriesId = UUID.randomUUID().toString();
        String draftId = UUID.randomUUID().toString();

        DraftState game1 = draftService.createFearlessDraft(
                draftId,
                req.getBlueTeamName(),
                req.getRedTeamName(),
                req.getFirstPickTeam(),
                seriesId,
                1,
                List.of()
        );

        SeriesState s = new SeriesState(
                seriesId,
                req.getBlueTeamName(),
                req.getRedTeamName(),
                req.getFirstPickTeam(),
                req.getBestOf(),
                1,
                draftId,
                new HashSet<>()
        );

        seriesMap.put(seriesId, s);

        // store + broadcast the new draft (timer starts only after both teams ready)
        draftManager.registerDraft(game1);

        return game1;
    }

    public DraftState nextGame(String seriesId) {
        SeriesState s = seriesMap.get(seriesId);
        if (s == null) throw new IllegalArgumentException("Series not found: " + seriesId);

        if (s.currentGame() >= s.bestOf()) {
            throw new IllegalStateException("Series is already complete");
        }

        DraftState currentDraft = draftManager.get(s.currentDraftId());
        if (currentDraft.phase() != DraftPhase.COMPLETE) {
            throw new IllegalStateException("Current game is not complete yet");
        }

        // picks-only fearless locks (exclude NONE)
        Set<String> newLocked = new HashSet<>(s.lockedChampionIds());
        for (String id : currentDraft.bluePicks()) {
            if (!DraftConstants.NONE_CHAMPION_ID.equals(id)) newLocked.add(id);
        }
        for (String id : currentDraft.redPicks()) {
            if (!DraftConstants.NONE_CHAMPION_ID.equals(id)) newLocked.add(id);
        }

        int nextGameNum = s.currentGame() + 1;
        String nextDraftId = UUID.randomUUID().toString();

        DraftState nextDraft = draftService.createFearlessDraft(
                nextDraftId,
                s.blueTeamName(),
                s.redTeamName(),
                s.firstPickTeam(),
                s.seriesId(),
                nextGameNum,
                List.copyOf(newLocked)
        );

        SeriesState updatedSeries = new SeriesState(
                s.seriesId(),
                s.blueTeamName(),
                s.redTeamName(),
                s.firstPickTeam(),
                s.bestOf(),
                nextGameNum,
                nextDraftId,
                newLocked
        );

        seriesMap.put(seriesId, updatedSeries);

        // store + broadcast next game draft (ready-check will start it)
        draftManager.registerDraft(nextDraft);

        return nextDraft;
    }

    public SeriesState getSeries(String seriesId) {
        SeriesState s = seriesMap.get(seriesId);
        if (s == null) throw new IllegalArgumentException("Series not found: " + seriesId);
        return s;
    }

    @EventListener
    public void onDraftCompleted(DraftCompletedEvent event) {
        DraftState state = event.state();
        if (state.mode() != DraftMode.FEARLESS_SERIES || state.seriesId() == null) return;

        SeriesState series = seriesMap.get(state.seriesId());
        if (series == null) return;
        if (!series.currentDraftId().equals(state.draftId())) return;
        if (series.currentGame() >= series.bestOf()) return;

        DraftState nextDraft = nextGame(series.seriesId());
        DraftState payload = draftManager.getForClient(nextDraft.draftId());
        brokerMessagingTemplate.convertAndSend(
                "/topic/series/" + series.seriesId(),
                new SeriesNextGame(series.seriesId(), payload)
        );
    }
}
