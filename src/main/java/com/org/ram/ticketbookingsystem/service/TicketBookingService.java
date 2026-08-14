package com.org.ram.ticketbookingsystem.service;

import com.org.ram.ticketbookingsystem.entity.Seat;

public interface TicketBookingService {
    Seat bookTicket(final Long seatId, final Long showId);
}
