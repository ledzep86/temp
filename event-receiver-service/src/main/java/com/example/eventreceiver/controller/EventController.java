package com.example.eventreceiver.controller;

import com.example.eventreceiver.service.EventService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/api/events")
public class EventController {

    @Autowired
    private EventService eventService;

    @Value("${valid.customer.tiers}")
    private List<String> validCustomerTiers;

    @PostMapping("/receive")
    public ResponseEntity<String> receiveEvent(@RequestBody String eventPayload, HttpServletRequest request) {
        String customerTier = request.getHeader("X-Customer-Tier");
        if (customerTier == null || !validCustomerTiers.contains(customerTier)) {
            return new ResponseEntity<>("Invalid or missing X-Customer-Tier header", HttpStatus.BAD_REQUEST);
        }

        eventService.processEvent(eventPayload);
        return new ResponseEntity<>("Event received successfully", HttpStatus.OK);
    }
}