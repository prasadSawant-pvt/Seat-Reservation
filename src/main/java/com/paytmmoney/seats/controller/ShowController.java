package com.paytmmoney.seats.controller;

import com.paytmmoney.seats.dto.CreateShowRequest;
import com.paytmmoney.seats.dto.ShowResponse;
import com.paytmmoney.seats.dto.ShowStateResponse;
import com.paytmmoney.seats.service.ShowService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/shows")
public class ShowController {

    private final ShowService showService;

    public ShowController(ShowService showService) {
        this.showService = showService;
    }

    @PostMapping
    public ResponseEntity<ShowResponse> createShow(@Valid @RequestBody CreateShowRequest request) {
        ShowResponse response = showService.createShow(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ShowStateResponse> getShowState(@PathVariable UUID id) {
        ShowStateResponse response = showService.getShowState(id);
        return ResponseEntity.ok(response);
    }
}
