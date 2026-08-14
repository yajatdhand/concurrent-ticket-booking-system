package com.org.ram.ticketbookingsystem.entity;

import com.org.ram.ticketbookingsystem.model.BookingStatus;
import jakarta.persistence.*;

@Entity
public class Seat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long seatId;

    private String seatNumber;

    @ManyToOne
    @JoinColumn(name = "show_id")
    private Show show;

    @Enumerated(EnumType.STRING)
    private BookingStatus bookingStatus = BookingStatus.AVAILABLE;

    @Version
    private Integer version;

    protected Seat() {
    }

    public Seat(String seatNumber, Show show) {
        this.seatNumber = seatNumber;
        this.show = show;
    }

    public String getSeatNumber() {
        return seatNumber;
    }

    public void setSeatNumber(String seatNumber) {
        this.seatNumber = seatNumber;
    }

    public Show getShow() {
        return show;
    }

    public void setShow(Show show) {
        this.show = show;
    }

    public BookingStatus getBookingStatus() {
        return bookingStatus;
    }

    public void setBookingStatus(BookingStatus bookingStatus) {
        this.bookingStatus = bookingStatus;
    }

    @Override
    public String toString() {
        return "Seat{" +
                "seatId=" + seatId +
                ", seatNumber='" + seatNumber + '\'' +
                ", show=" + show +
                ", bookingStatus=" + bookingStatus +
                '}';
    }
}
