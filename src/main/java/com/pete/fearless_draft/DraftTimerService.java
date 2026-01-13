package com.pete.fearless_draft;

import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

@Service
public class DraftTimerService {

    private final TaskScheduler scheduler;
    private final DraftTimeoutHandler timeoutHandler;

    private final Map<String, ScheduledFuture<?>> timers = new ConcurrentHashMap<>();

    public DraftTimerService(TaskScheduler scheduler, @Lazy DraftTimeoutHandler timeoutHandler) {
        this.scheduler = scheduler;
        this.timeoutHandler = timeoutHandler;
    }

    public void schedule(DraftState state) {
        cancel(state.draftId());

        if (state.phase() == DraftPhase.COMPLETE) return;

        final String draftId = state.draftId();
        final int expectedStep = state.step();
        final DraftPhase expectedPhase = state.phase();
        final long expectedStartedAt = state.turnStartedAt();

        Instant fireAt = Instant.ofEpochMilli(expectedStartedAt)
                .plusSeconds(state.turnDurationSeconds());

        ScheduledFuture<?> future = scheduler.schedule(() -> {
            timeoutHandler.onTurnTimeout(draftId, expectedPhase, expectedStep, expectedStartedAt);
        }, fireAt);

        timers.put(draftId, future);
    }

    public void cancel(String draftId) {
        ScheduledFuture<?> existing = timers.remove(draftId);
        if (existing != null) existing.cancel(false);
    }
}
