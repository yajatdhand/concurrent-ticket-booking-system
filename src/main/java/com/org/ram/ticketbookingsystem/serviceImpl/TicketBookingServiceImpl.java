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
    public Seat bookTicket(Long seatId, Long showId) {
        Seat response;
        Optional<Seat> seat = seatRepository.findBySeatIdAndShow_ShowId(seatId, showId);
        if (seat.isPresent()) {
            Seat entity = seat.get();
            if (entity.getBookingStatus() == BookingStatus.AVAILABLE) {
                entity.setBookingStatus(BookingStatus.BOOKED);
                response = seatRepository.save(entity);
            } else {
                throw new SeatAlreadyBookedException("Seat is already booked!");
            }
        } else {
            throw new SeatNotFoundException("Seat not found!");
        }
        return response;
    }
}
