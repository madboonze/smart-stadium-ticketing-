package com.smartstadium.domain.booking;

import com.smartstadium.domain.match.Match;
import com.smartstadium.domain.seat.RegularSeat;
import com.smartstadium.domain.seat.Seat;
import com.smartstadium.domain.seat.SeatStatus;
import com.smartstadium.domain.stadium.Stadium;
import com.smartstadium.domain.user.User;
import com.smartstadium.domain.user.UserRole;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BookingTest {
    @Test
    void pendingBookingHoldsSeatAndConfirmationBooksIt() {
        Seat seat = new RegularSeat("R1", new BigDecimal("1000"));
        User user = new User("u1", "Test Customer", "test@example.com", UserRole.CUSTOMER);
        Stadium stadium = new Stadium("s1", "Test Arena", "Dubai");
        Match match = new Match("m1", "Home", "Away", LocalDateTime.now().plusDays(1), stadium);

        Booking booking = new Booking(user, match, List.of(seat));

        assertEquals(SeatStatus.HELD, seat.getStatus());
        booking.confirm();

        assertEquals(BookingStatus.CONFIRMED, booking.getStatus());
        assertEquals(SeatStatus.BOOKED, seat.getStatus());
        assertEquals(1, booking.getTickets().size());
    }

    @Test
    void cancellingPendingBookingReleasesSeat() {
        Seat seat = new RegularSeat("R1", new BigDecimal("1000"));
        User user = new User("u1", "Test Customer", "test@example.com", UserRole.CUSTOMER);
        Stadium stadium = new Stadium("s1", "Test Arena", "Dubai");
        Match match = new Match("m1", "Home", "Away", LocalDateTime.now().plusDays(1), stadium);

        Booking booking = new Booking(user, match, List.of(seat));
        booking.cancel();

        assertEquals(SeatStatus.AVAILABLE, seat.getStatus());
        assertEquals(BookingStatus.CANCELLED, booking.getStatus());
    }

    @Test
    void cannotBookAlreadyHeldSeat() {
        Seat seat = new RegularSeat("R1", new BigDecimal("1000"));
        seat.hold();

        User user = new User("u1", "Test Customer", "test@example.com", UserRole.CUSTOMER);
        Stadium stadium = new Stadium("s1", "Test Arena", "Dubai");
        Match match = new Match("m1", "Home", "Away", LocalDateTime.now().plusDays(1), stadium);

        assertThrows(IllegalArgumentException.class,
                () -> new Booking(user, match, List.of(seat)));
    }
}
