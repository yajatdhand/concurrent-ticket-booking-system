package com.org.ram.ticketbookingsystem.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.time.Instant;

@Entity
public class Show {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long showId;

    private String movieName;

    private Instant showTime;

    protected Show() {
    }

    public Show(String movieName, Instant showTime) {
        this.movieName = movieName;
        this.showTime = showTime;
    }

    public String getMovieName() {
        return movieName;
    }

    public void setMovieName(String movieName) {
        this.movieName = movieName;
    }

    public Instant getShowTime() {
        return showTime;
    }

    public void setShowTime(Instant showTime) {
        this.showTime = showTime;
    }

    @Override
    public String toString() {
        return "Show{" +
                "showId=" + showId +
                ", movieName='" + movieName + '\'' +
                ", showTime=" + showTime +
                '}';
    }
}
