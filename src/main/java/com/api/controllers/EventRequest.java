package com.api.controllers;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EventRequest
{
    private String summary;
    private String description;
    private String location;
    private String start;
    private String end;
    private String organizerEmail;
    private List<String> attendeeEmail;
}
