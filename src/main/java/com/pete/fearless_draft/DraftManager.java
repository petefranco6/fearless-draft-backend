package com.pete.fearless_draft;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class DraftManager implements DraftTimeoutHandler {

    private final DraftService draftService;
    private final DraftTimerService timerService;
    private final SimpMessagingTemplate brokerMessagingTemplate;

    private final Map<String, DraftState> drafts = new ConcurrentHashMap<>();

    private static final int TURN_DURATION_SECONDS = 30;

    public DraftManager(
            DraftService draftService,
            DraftTimerService timerService,
            SimpMessagingTemplate brokerMessagingTemplate
    ) {
        this.draftService = draftService;
        this.timerService = timerService;
        this.brokerMessagingTemplate = brokerMessagingTemplate;
    }

    private boolean isStarted(DraftState s) {
        return s.turnStartedAt() > 0 && s.turnDurationSeconds() > 0;
    }

    private boolean bothReady(DraftState s) {
        return s.blueReady() && s.redReady();
    }

    /* ---------------- CREATE ---------------- */

    // Create SINGLE draft but DO NOT start timer
    public DraftState createNewDraft(String blueTeamName, String redTeamName, DraftTurn firstPickTeam) {
        String draftId = UUID.randomUUID().toString();

        DraftState state = draftService.createDraft(draftId, blueTeamName, redTeamName, firstPickTeam);
        drafts.put(draftId, state);

        // broadcast immediately so clients can lobby/ready-up
        broadcast(state);

        return state;
    }

    /**
     * ✅ Used by SeriesManager (or any future mode) to register a draft in the same storage + broadcast pipeline.
     * Does NOT start the timer.
     */
    public void registerDraft(DraftState state) {
        drafts.put(state.draftId(), state);
        broadcast(state);
    }

    /* ---------------- START ---------------- */

    /**
     * Start draft timer (REST safety valve).
     * We still enforce ready-check so clients can't bypass it.
     */
    public DraftState startDraft(String draftId) {
        DraftState current = get(draftId);

        if (current.phase() == DraftPhase.COMPLETE) return current;
        if (isStarted(current)) return current;

        if (!bothReady(current)) {
            // ignore until both ready
            return current;
        }

        DraftState started = stampTurnTiming(current);

        drafts.put(draftId, started);
        timerService.schedule(started);
        broadcast(started);

        return started;
    }

    /* ---------------- READ ---------------- */

    public DraftState get(String draftId) {
        DraftState state = drafts.get(draftId);
        if (state == null) throw new IllegalArgumentException("Draft not found: " + draftId);
        return state;
    }

    /* ---------------- READY CHECK ---------------- */

    public void setReady(String draftId, DraftTurn team, boolean ready) {
        DraftState current = get(draftId);

        if (current.phase() == DraftPhase.COMPLETE) return;

        // once started, ignore ready toggles
        if (isStarted(current)) return;

        boolean blueReady = current.blueReady();
        boolean redReady = current.redReady();

        if (team == DraftTurn.BLUE) blueReady = ready;
        if (team == DraftTurn.RED)  redReady = ready;

        DraftState updated = new DraftState(
                current.draftId(),
                current.blueTeamName(),
                current.redTeamName(),
                current.firstPickTeam(),
                current.phase(),
                current.step(),
                current.turn(),
                current.bluePicks(),
                current.redPicks(),
                current.bans(),
                current.previews(),
                current.lastPickedChampion(),
                current.turnStartedAt(),
                current.turnDurationSeconds(),
                0L,   // serverNow - filled at broadcast time
                0L,   // turnEndsAt - filled at broadcast time
                blueReady,
                redReady,

                // ✅ fearless / mode fields preserved
                current.mode(),
                current.seriesId(),
                current.gameNumber(),
                current.lockedChampionIds()
        );

        // if both ready -> auto start
        if (blueReady && redReady) {
            DraftState started = stampTurnTiming(updated);
            drafts.put(draftId, started);
            timerService.schedule(started);
            broadcast(started);
            return;
        }

        drafts.put(draftId, updated);
        broadcast(updated);
    }

    /* ---------------- MUTATIONS ---------------- */

    public void applyAction(String draftId, DraftAction action) {
        DraftState current = get(draftId);
        if (current.phase() == DraftPhase.COMPLETE) return;

        if (!isStarted(current)) {
            return;
        }

        DraftState updated = draftService.applyAction(current, action);
        updated = stampTurnTiming(updated);

        drafts.put(draftId, updated);
        timerService.schedule(updated);
        broadcast(updated);
    }

    public void setPreview(String draftId, DraftTurn team, String championId) {
        DraftState current = get(draftId);

        if (!isStarted(current)) {
            return;
        }

        DraftState updated = draftService.setPreview(current, team, championId);
        drafts.put(draftId, updated);
        broadcast(updated);
    }

    /* ---------------- TIMEOUT ---------------- */

    @Override
    public void onTurnTimeout(String draftId, DraftPhase expectedPhase, int expectedStep, long expectedStartedAt) {
        DraftState current = drafts.get(draftId);
        if (current == null) return;

        if (current.phase() != expectedPhase) return;
        if (current.step() != expectedStep) return;
        if (current.turnStartedAt() != expectedStartedAt) return;
        if (current.phase() == DraftPhase.COMPLETE) return;

        DraftAction auto = buildAutoAction(current);

        DraftAction actionToApply = (auto != null)
                ? auto
                : new DraftAction(draftId, current.turn(), DraftConstants.NONE_CHAMPION_ID);

        DraftState next = draftService.applyAction(current, actionToApply);
        DraftState updated = stampTurnTiming(next);

        drafts.put(draftId, updated);
        timerService.schedule(updated);
        broadcast(updated);
    }

    /* ---------------- HELPERS ---------------- */

    private DraftState withServerTime(DraftState s) {
        long now = System.currentTimeMillis();
        long endsAt = (s.turnStartedAt() > 0 && s.turnDurationSeconds() > 0)
                ? s.turnStartedAt() + (s.turnDurationSeconds() * 1000L)
                : 0L;

        return new DraftState(
                s.draftId(),
                s.blueTeamName(),
                s.redTeamName(),
                s.firstPickTeam(),
                s.phase(),
                s.step(),
                s.turn(),
                s.bluePicks(),
                s.redPicks(),
                s.bans(),
                s.previews(),
                s.lastPickedChampion(),
                s.turnStartedAt(),
                s.turnDurationSeconds(),
                now,
                endsAt,
                s.blueReady(),
                s.redReady(),
                s.mode(),
                s.seriesId(),
                s.gameNumber(),
                s.lockedChampionIds()
        );
    }

    private void broadcast(DraftState state) {
        brokerMessagingTemplate.convertAndSend("/topic/draft/" + state.draftId(), withServerTime(state));
    }

    private DraftState stampTurnTiming(DraftState state) {
        if (state.phase() == DraftPhase.COMPLETE) {
            // Stored state can be zeros; broadcast() will enrich serverNow accurately
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
                    state.previews(),
                    state.lastPickedChampion(),
                    0L,
                    0,
                    0L,   // serverNow
                    0L,   // turnEndsAt
                    state.blueReady(),
                    state.redReady(),
                    state.mode(),
                    state.seriesId(),
                    state.gameNumber(),
                    state.lockedChampionIds()
            );
        }

        long now = System.currentTimeMillis();
        long endsAt = now + (TURN_DURATION_SECONDS * 1000L);

        // Store the true turnStartedAt/duration; serverNow/turnEndsAt may be filled at broadcast-time too
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
                state.previews(),
                state.lastPickedChampion(),
                now,
                TURN_DURATION_SECONDS,
                0L,   // serverNow
                0L,   // turnEndsAt
                state.blueReady(),
                state.redReady(),
                state.mode(),
                state.seriesId(),
                state.gameNumber(),
                state.lockedChampionIds()
        );
    }

    private DraftAction buildAutoAction(DraftState state) {
        String preview = state.previews().get(state.turn());
        if (preview == null || preview.isBlank()) return null;
        return new DraftAction(state.draftId(), state.turn(), preview);
    }

    public DraftState getForClient(String draftId) {
        return withServerTime(get(draftId));
    }

}
