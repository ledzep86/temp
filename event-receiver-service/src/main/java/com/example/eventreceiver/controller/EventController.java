package com.example.eventreceiver.controller;

import com.example.eventreceiver.model.EventRequest;
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
    public ResponseEntity<String> receiveEvent(@RequestBody EventRequest eventRequest, @RequestHeader(value = "X-Customer-Tier", required = false) String customerTier, HttpServletRequest request) {
        if (customerTier == null || !validCustomerTiers.contains(customerTier)) {
            return new ResponseEntity<>("Invalid or missing X-Customer-Tier header", HttpStatus.BAD_REQUEST);
        }

        int contentLength = request.getContentLength();
        if (contentLength < 1024 || contentLength > 100 * 1024 * 1024) { // 1KB to 100MB
            return new ResponseEntity<>("Request body size must be between 1KB and 100MB", HttpStatus.BAD_REQUEST);
        }

        eventService.processEvent(eventRequest.getBody());
        return new ResponseEntity<>("Event received successfully", HttpStatus.OK);
    }
}