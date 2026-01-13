package com.pete.fearless_draft.series;

import com.pete.fearless_draft.DraftTurn;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class CreateSeriesRequest {
    private String blueTeamName;
    private String redTeamName;
    private DraftTurn firstPickTeam;
    private int bestOf;

}
