package com.pete.fearless_draft.series;

import com.pete.fearless_draft.CreateDraftRequest;
import com.pete.fearless_draft.DraftState;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/series")
public class SeriesController {

    private final SeriesManager seriesManager;

    public SeriesController(SeriesManager seriesManager) {
        this.seriesManager = seriesManager;
    }

    @PostMapping
    public DraftState createSeries(@RequestBody CreateSeriesRequest request) {
        return seriesManager.createSeries(request);
    }

    @PostMapping("/{seriesId}/next")
    public DraftState nextGame(@PathVariable String seriesId, @RequestBody CreateDraftRequest req) {
        return seriesManager.nextGame(seriesId, req);
    }

    @GetMapping("/{seriesId}")
    public SeriesState getSeries(@PathVariable String seriesId) {
        return seriesManager.getSeries(seriesId);
    }
}
