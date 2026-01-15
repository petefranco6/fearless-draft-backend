package com.pete.fearless_draft.series;

import com.pete.fearless_draft.*;
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

    public SeriesManager(DraftService draftService, DraftManager draftManager, SimpMessagingTemplate brokerMessagingTemplate) {
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

        draftManager.registerDraft(game1);

        brokerMessagingTemplate.convertAndSend(
                "/topic/series/" + seriesId,
                new SeriesDraftCreatedEvent("SERIES_DRAFT_CREATED", seriesId, 1, draftId, "UNKOWN")
        );

        return game1;
    }

    /**
     * Create the next game's draft using submitted settings (like CreateDraftPage).
     */
    public DraftState nextGame(String seriesId, CreateDraftRequest req) {
        SeriesState s = seriesMap.get(seriesId);
        if (s == null) throw new IllegalArgumentException("Series not found: " + seriesId);

        if (s.currentGame() >= s.bestOf()) {
            throw new IllegalStateException("Series is already complete");
        }

        DraftState currentDraft = draftManager.get(s.currentDraftId());
        if (currentDraft.phase() != DraftPhase.COMPLETE) {
            throw new IllegalStateException("Current game is not complete yet");
        }

        // Validate request
        String blueName = req.getBlueTeamName() == null ? "" : req.getBlueTeamName().trim();
        String redName  = req.getRedTeamName() == null ? "" : req.getRedTeamName().trim();

        if (blueName.isBlank() || redName.isBlank()) {
            throw new IllegalArgumentException("Team names are required");
        }
        if (req.getFirstPickTeam() == null) {
            throw new IllegalArgumentException("firstPickTeam is required");
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
                blueName,
                redName,
                req.getFirstPickTeam(),
                s.seriesId(),
                nextGameNum,
                List.copyOf(newLocked)
        );

        // Update series settings to match what the user just chose
        SeriesState updatedSeries = new SeriesState(
                s.seriesId(),
                blueName,
                redName,
                req.getFirstPickTeam(),
                s.bestOf(),
                nextGameNum,
                nextDraftId,
                newLocked
        );

        seriesMap.put(seriesId, updatedSeries);

        draftManager.registerDraft(nextDraft);

        brokerMessagingTemplate.convertAndSend(
                "/topic/series/" + seriesId,
                new SeriesDraftCreatedEvent("SERIES_DRAFT_CREATED", seriesId, nextGameNum, nextDraftId, "UNKNOWN")
        );

        return nextDraft;
    }


    public SeriesState getSeries(String seriesId) {
        SeriesState s = seriesMap.get(seriesId);
        if (s == null) throw new IllegalArgumentException("Series not found: " + seriesId);
        return s;
    }
}
