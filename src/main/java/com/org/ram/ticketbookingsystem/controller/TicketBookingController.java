package com.org.ram.ticketbookingsystem.controller;

import com.org.ram.ticketbookingsystem.dto.BookTicketRequest;
import com.org.ram.ticketbookingsystem.dto.BookTicketResponse;
import com.org.ram.ticketbookingsystem.entity.Seat;
import com.org.ram.ticketbookingsystem.service.TicketBookingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ticket-booking-service")
public class TicketBookingController {
    private final TicketBookingService ticketBookingService;

    public TicketBookingController(TicketBookingService ticketBookingService) {
        this.ticketBookingService = ticketBookingService;
    }

    @PostMapping("book-ticket-optimistic")
    public ResponseEntity<BookTicketResponse> bookTicketOptimistic(@RequestBody BookTicketRequest request) {
        Seat seat = ticketBookingService.bookTicketOptimistically(request.seatId(), request.showId());
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new BookTicketResponse(seat.getSeatNumber(), seat.getShow().getMovieName(), seat.getBookingStatus()));
    }

    @PostMapping("book-ticket-pessimistic")
    public ResponseEntity<BookTicketResponse> bookTicketPessimistic(@RequestBody BookTicketRequest request) {
        Seat seat = ticketBookingService.bookTicketPessimistically(request.seatId(), request.showId());
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new BookTicketResponse(seat.getSeatNumber(), seat.getShow().getMovieName(), seat.getBookingStatus()));
    }
}
