package com.org.ram.ticketbookingsystem.dto;

import com.org.ram.ticketbookingsystem.model.BookingStatus;

public record BookTicketResponse(String seatNumber, String movieName, BookingStatus status) {
}
