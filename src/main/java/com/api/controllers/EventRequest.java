package com.api.controllers;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.api.services.calendar.model.EventAttachment;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

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
