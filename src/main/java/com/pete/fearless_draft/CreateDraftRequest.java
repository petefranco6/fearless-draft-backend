package com.pete.fearless_draft;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class CreateDraftRequest {
    // Getters & Setters
    private String blueTeamName;
    private String redTeamName;
    private DraftTurn firstPickTeam; // "BLUE" or "RED"

}
