package com.org.ram.ticketbookingsystem.service;

import com.org.ram.ticketbookingsystem.entity.Seat;

public interface TicketBookingService {
    Seat bookTicketOptimistically(final Long seatId, final Long showId);

    Seat bookTicketPessimistically(final Long seatId, final Long showId);
}
