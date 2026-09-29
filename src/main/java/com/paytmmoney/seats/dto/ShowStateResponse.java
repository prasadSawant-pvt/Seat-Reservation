package com.paytmmoney.seats.dto;

import java.time.Instant;
import java.util.List;

public class ShowStateResponse {

    private String id;
    private String name;
    private Long pricePaise;
    private Integer perUserLimit;
    private Instant createdAt;
    private Counts counts;
    private List<SeatState> seats;

    public ShowStateResponse() {
    }

    public ShowStateResponse(String id, String name, Long pricePaise, Integer perUserLimit, Instant createdAt, Counts counts, List<SeatState> seats) {
        this.id = id;
        this.name = name;
        this.pricePaise = pricePaise;
        this.perUserLimit = perUserLimit;
        this.createdAt = createdAt;
        this.counts = counts;
        this.seats = seats;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getPricePaise() {
        return pricePaise;
    }

    public void setPricePaise(Long pricePaise) {
        this.pricePaise = pricePaise;
    }

    public Integer getPerUserLimit() {
        return perUserLimit;
    }

    public void setPerUserLimit(Integer perUserLimit) {
        this.perUserLimit = perUserLimit;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Counts getCounts() {
        return counts;
    }

    public void setCounts(Counts counts) {
        this.counts = counts;
    }

    public List<SeatState> getSeats() {
        return seats;
    }

    public void setSeats(List<SeatState> seats) {
        this.seats = seats;
    }

    public static class Counts {
        private long available;
        private long held;
        private long confirmed;
        private long total;

        public Counts() {
        }

        public Counts(long available, long held, long confirmed, long total) {
            this.available = available;
            this.held = held;
            this.confirmed = confirmed;
            this.total = total;
        }

        public long getAvailable() {
            return available;
        }

        public void setAvailable(long available) {
            this.available = available;
        }

        public long getHeld() {
            return held;
        }

        public void setHeld(long held) {
            this.held = held;
        }

        public long getConfirmed() {
            return confirmed;
        }

        public void setConfirmed(long confirmed) {
            this.confirmed = confirmed;
        }

        public long getTotal() {
            return total;
        }

        public void setTotal(long total) {
            this.total = total;
        }
    }

    public static class SeatState {
        private String seatLabel;
        private String status;

        public SeatState() {
        }

        public SeatState(String seatLabel, String status) {
            this.seatLabel = seatLabel;
            this.status = status;
        }

        public String getSeatLabel() {
            return seatLabel;
        }

        public void setSeatLabel(String seatLabel) {
            this.seatLabel = seatLabel;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }
}
