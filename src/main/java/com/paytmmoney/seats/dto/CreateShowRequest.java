package com.paytmmoney.seats.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

public class CreateShowRequest {

    @NotEmpty(message = "name is required")
    private String name;

    @NotEmpty(message = "seats list is required")
    private List<String> seats;

    @NotNull(message = "price_paise is required")
    @Positive(message = "price_paise must be positive")
    @JsonProperty("price_paise")
    private Long pricePaise;

    @JsonProperty("per_user_limit")
    private Integer perUserLimit;

    public CreateShowRequest() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getSeats() {
        return seats;
    }

    public void setSeats(List<String> seats) {
        this.seats = seats;
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
}
