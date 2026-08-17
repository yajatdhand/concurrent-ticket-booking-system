package com.org.ram.ticketbookingsystem.serviceImpl;

import com.org.ram.ticketbookingsystem.entity.Seat;
import com.org.ram.ticketbookingsystem.exceptions.SeatAlreadyBookedException;
import com.org.ram.ticketbookingsystem.exceptions.SeatNotFoundException;
import com.org.ram.ticketbookingsystem.model.BookingStatus;
import com.org.ram.ticketbookingsystem.repository.SeatRepository;
import com.org.ram.ticketbookingsystem.service.TicketBookingService;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class TicketBookingServiceImpl implements TicketBookingService {

    private final SeatRepository seatRepository;

    public TicketBookingServiceImpl(final SeatRepository seatRepository) {
        this.seatRepository = seatRepository;
    }

    @Override
    @Transactional
    public Seat bookTicketOptimistically(Long seatId, Long showId) {
        Optional<Seat> seat = seatRepository.findBySeatIdAndShow_ShowId(seatId, showId);
        return processBooking(seat);
    }

    @Override
    @Transactional
    public Seat bookTicketPessimistically(Long seatId, Long showId) {
        Optional<Seat> seat = seatRepository.findPessimisticBySeatIdAndShow_ShowId(seatId, showId);
        return processBooking(seat);
    }

    private Seat processBooking(Optional<Seat> seat) {
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        if (seat.isEmpty()) throw new SeatNotFoundException("Seat not found!");

        Seat entity = seat.get();
        if (entity.getBookingStatus() != BookingStatus.AVAILABLE)
            throw new SeatAlreadyBookedException("Seat is already booked!");

        entity.setBookingStatus(BookingStatus.BOOKED);
        return entity;
    }
}
