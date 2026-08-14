package com.org.ram.ticketbookingsystem.controller;

import com.org.ram.ticketbookingsystem.dto.BookingErrorResponse;
import com.org.ram.ticketbookingsystem.exceptions.SeatAlreadyBookedException;
import com.org.ram.ticketbookingsystem.exceptions.SeatNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler
    public ResponseEntity<BookingErrorResponse> handle(SeatNotFoundException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new BookingErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler
    public ResponseEntity<BookingErrorResponse> handle(SeatAlreadyBookedException ex) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(new BookingErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler
    public ResponseEntity<BookingErrorResponse> handle(ObjectOptimisticLockingFailureException ex) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(new BookingErrorResponse("Seat was modified by another request. Please try again."));
    }
}
