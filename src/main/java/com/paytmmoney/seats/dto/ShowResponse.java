package com.paytmmoney.seats.dto;

import java.time.Instant;
import java.util.List;

public class ShowResponse {

    private String id;
    private String name;
    private Long pricePaise;
    private Integer perUserLimit;
    private Instant createdAt;
    private List<SeatInfo> seats;

    public ShowResponse() {
    }

    public ShowResponse(java.util.UUID id, String name, Long pricePaise, Integer perUserLimit, Instant createdAt, List<SeatInfo> seats) {
        this.id = id != null ? id.toString() : null;
        this.name = name;
        this.pricePaise = pricePaise;
        this.perUserLimit = perUserLimit;
        this.createdAt = createdAt;
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

    public List<SeatInfo> getSeats() {
        return seats;
    }

    public void setSeats(List<SeatInfo> seats) {
        this.seats = seats;
    }

    public static class SeatInfo {
        private String seatLabel;
        private String status;

        public SeatInfo() {
        }

        public SeatInfo(String seatLabel, String status) {
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
