package com.pete.fearless_draft;

import static com.pete.fearless_draft.DraftPhase.*;
import static com.pete.fearless_draft.DraftTurn.*;

public class DraftOrder {

    // Builds the SAME pattern you already have, but relative to who is "first" vs "second"
    public static DraftStep[] build(DraftTurn firstPickTeam) {
        DraftTurn first = firstPickTeam;
        DraftTurn second = (firstPickTeam == BLUE) ? RED : BLUE;

        return new DraftStep[] {
                // Ban phase 1 (first, second alternating)
                new DraftStep(BAN, first),
                new DraftStep(BAN, second),
                new DraftStep(BAN, first),
                new DraftStep(BAN, second),
                new DraftStep(BAN, first),
                new DraftStep(BAN, second),

                // Pick phase 1 (same pattern you hard-coded, relative to first/second)
                new DraftStep(PICK, first),
                new DraftStep(PICK, second),
                new DraftStep(PICK, second),
                new DraftStep(PICK, first),
                new DraftStep(PICK, first),
                new DraftStep(PICK, second),

                // Ban phase 2 (same as your hard-coded: second, first, second, first)
                new DraftStep(BAN, second),
                new DraftStep(BAN, first),
                new DraftStep(BAN, second),
                new DraftStep(BAN, first),

                // Pick phase 2 (same as your hard-coded: second, first, first, second)
                new DraftStep(PICK, second),
                new DraftStep(PICK, first),
                new DraftStep(PICK, first),
                new DraftStep(PICK, second)
        };
    }
}
