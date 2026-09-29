package com.paytmmoney.seats.service;

import com.paytmmoney.seats.dto.CreateShowRequest;
import com.paytmmoney.seats.dto.ShowResponse;
import com.paytmmoney.seats.dto.ShowStateResponse;
import com.paytmmoney.seats.entity.Seat;
import com.paytmmoney.seats.entity.Show;
import com.paytmmoney.seats.repository.SeatRepository;
import com.paytmmoney.seats.repository.ShowRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
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
        if (uniqueSeats.size() != request.getSeats().size()) {
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

    @Transactional(readOnly = true)
    public ShowStateResponse getShowState(UUID showId) {
        Show show = showRepository.findById(showId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Show not found"));

        List<Object[]> seatRows = seatRepository.findSeatStatesByShowId(showId);

        long available = seatRows.stream().filter(row -> row[1].toString().equals("available")).count();
        long held = seatRows.stream().filter(row -> row[1].toString().equals("held")).count();
        long confirmed = seatRows.stream().filter(row -> row[1].toString().equals("confirmed")).count();

        List<ShowStateResponse.SeatState> seatStates = seatRows.stream()
                .map(row -> new ShowStateResponse.SeatState((String) row[0], row[1].toString()))
                .collect(Collectors.toList());

        ShowStateResponse.Counts counts = new ShowStateResponse.Counts(available, held, confirmed, seatRows.size());

        return new ShowStateResponse(
                show.getId().toString(),
                show.getName(),
                show.getPricePaise(),
                show.getPerUserLimit(),
                show.getCreatedAt(),
                counts,
                seatStates
        );
    }
}
