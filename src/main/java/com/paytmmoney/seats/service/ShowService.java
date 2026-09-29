package com.paytmmoney.seats.service;

import com.paytmmoney.seats.dto.CreateShowRequest;
import com.paytmmoney.seats.dto.ShowResponse;
import com.paytmmoney.seats.entity.Seat;
import com.paytmmoney.seats.entity.Show;
import com.paytmmoney.seats.repository.SeatRepository;
import com.paytmmoney.seats.repository.ShowRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ShowService {

    private final ShowRepository showRepository;
    private final SeatRepository seatRepository;

    public ShowService(ShowRepository showRepository, SeatRepository seatRepository) {
        this.showRepository = showRepository;
        this.seatRepository = seatRepository;
    }

    @Transactional
    public ShowResponse createShow(CreateShowRequest request) {
        // Validate unique seats - must happen before any DB operation
        Set<String> uniqueSeats = new HashSet<>(request.getSeats());
        System.out.println("Request seats: " + request.getSeats() + ", Unique seats: " + uniqueSeats + ", Sizes: " + request.getSeats().size() + " vs " + uniqueSeats.size());
        if (uniqueSeats.size() != request.getSeats().size()) {
            System.out.println("THROWING: Duplicate seat labels detected");
            throw new IllegalArgumentException("Duplicate seat labels in request");
        }

        // Create show
        Show show = new Show(
                request.getName(),
                request.getPricePaise(),
                request.getPerUserLimit()
        );
        Show savedShow = showRepository.save(show);

        // Create seats in AVAILABLE status - use unique seats only
        List<Seat> seats = uniqueSeats.stream()
                .map(seatLabel -> {
                    Seat seat = new Seat(savedShow.getId(), seatLabel);
                    seat.setStatus(Seat.Status.available);
                    seat.setUpdatedAt(Instant.now());
                    return seat;
                })
                .collect(Collectors.toList());

        seatRepository.saveAll(seats);

        // Build response
        List<ShowResponse.SeatInfo> seatInfos = seats.stream()
                .map(seat -> new ShowResponse.SeatInfo(seat.getId().getSeatLabel(), seat.getStatus().name()))
                .collect(Collectors.toList());

        return new ShowResponse(
                savedShow.getId(),
                savedShow.getName(),
                savedShow.getPricePaise(),
                savedShow.getPerUserLimit(),
                savedShow.getCreatedAt(),
                seatInfos
        );
    }
}
