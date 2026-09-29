package com.paytmmoney.seats.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "shows")
public class Show {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private java.util.UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Long pricePaise;

    @Column(nullable = false)
    private Integer perUserLimit = 4;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public Show() {
    }

    public Show(String name, Long pricePaise, Integer perUserLimit) {
        this.name = name;
        this.pricePaise = pricePaise;
        this.perUserLimit = perUserLimit != null ? perUserLimit : 4;
    }

    public java.util.UUID getId() {
        return id;
    }

    public void setId(java.util.UUID id) {
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
}
