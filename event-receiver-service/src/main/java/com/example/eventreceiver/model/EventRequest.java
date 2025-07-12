package com.example.eventreceiver.model;

import lombok.Data;


@Data
public class EventRequest {

    private String eventTimestamp;
    private String body;
}