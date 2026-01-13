package com.pete.fearless_draft;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/draft")
public class DraftController {

    private final DraftManager draftManager;

    public DraftController(DraftManager draftManager) {
        this.draftManager = draftManager;
    }

    @PostMapping
    public DraftState createDraft(@RequestBody CreateDraftRequest request) {
        return draftManager.createNewDraft(
                request.getBlueTeamName(),
                request.getRedTeamName(),
                request.getFirstPickTeam()
        );
    }

    @PostMapping("/{draftId}/start")
    public  DraftState startDraft(@PathVariable String draftId) {
        return draftManager.startDraft(draftId);
    }

    @GetMapping("/{draftId}")
    public DraftState getDraft(@PathVariable String draftId) {
        return draftManager.get(draftId);
    }

}
