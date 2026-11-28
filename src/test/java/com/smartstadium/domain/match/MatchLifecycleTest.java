package com.smartstadium.domain.match;

import com.smartstadium.domain.stadium.Stadium;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class MatchLifecycleTest {
    private Match newMatch() {
        Stadium stadium = new Stadium("s1", "Grand Arena", "Dubai");
        return new Match("m1", "Falcons", "United",
                LocalDateTime.now().plusDays(3), stadium);
    }

    @Test
    void followsValidLifecycle() {
        Match match = newMatch();

        assertEquals(MatchStatus.SCHEDULED, match.getStatus());
        match.putOnSale();
        assertEquals(MatchStatus.ON_SALE, match.getStatus());
        match.markSoldOut();
        assertEquals(MatchStatus.SOLD_OUT, match.getStatus());
        match.markCompleted();
        assertEquals(MatchStatus.COMPLETED, match.getStatus());
    }

    @Test
    void preventsInvalidLifecycleTransition() {
        Match match = newMatch();

        assertThrows(IllegalStateException.class, match::markSoldOut);
        assertThrows(IllegalStateException.class, match::markCompleted);
    }

    @Test
    void preventsSameTeamPlayingItself() {
        Stadium stadium = new Stadium("s1", "Grand Arena", "Dubai");
        assertThrows(IllegalArgumentException.class, () ->
                new Match("m1", "Falcons", "falcons",
                        LocalDateTime.now().plusDays(3), stadium));
    }

    @Test
    void completedMatchCannotBeCancelled() {
        Match match = newMatch();
        match.putOnSale();
        match.markCompleted();

        assertThrows(IllegalStateException.class, match::cancel);
    }
}
