package com.org.ram.ticketbookingsystem.repository;

import com.org.ram.ticketbookingsystem.entity.Seat;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SeatRepository extends JpaRepository<Seat, Long> {

    Optional<Seat> findBySeatIdAndShow_ShowId(final Long seatId, final Long showId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Seat> findPessimisticBySeatIdAndShow_ShowId(final Long seatId, final Long showId);
}
