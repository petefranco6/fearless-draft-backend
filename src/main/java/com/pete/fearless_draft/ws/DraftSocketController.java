package com.pete.fearless_draft.ws;

import com.pete.fearless_draft.*;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

@Controller
public class DraftSocketController {

    private final DraftManager draftManager;

    public DraftSocketController(DraftManager draftManager) {
        this.draftManager = draftManager;
    }

    @MessageMapping("/draft/preview")
    public void handlePreview(DraftPreview preview) {
        // validates draft exists
        draftManager.get(preview.draftId());

        // DraftManager will broadcast updated state
        draftManager.setPreview(preview.draftId(), preview.team(), preview.championId());
    }

    @MessageMapping("/draft/action")
    public void handleDraftAction(DraftAction action) {
        // validates draft exists
        draftManager.get(action.draftId());

        // DraftManager will broadcast updated state
        draftManager.applyAction(action.draftId(), action);
    }

    @MessageMapping("/draft/ready")
    public void handleReady(DraftReadyMessage msg) {
        // validates draft exists
        draftManager.get(msg.draftId());

        // DraftManager will broadcast updated state (and auto-start if both ready)
        draftManager.setReady(msg.draftId(), msg.team(), msg.ready());
    }
}
