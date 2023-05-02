package com.api.controllers;

import com.google.api.client.auth.oauth2.AuthorizationCodeRequestUrl;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets.Details;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.Calendar.Events;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.*;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.gmail.model.Message;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.view.RedirectView;

import java.io.IOException;
import java.util.*;

@Controller
public class GoogleCalController {

    private final static Log logger = LogFactory.getLog(GoogleCalController.class);
    private static final String APPLICATION_NAME = "MyCalendarIntegration";
    private static HttpTransport httpTransport;
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static com.google.api.services.calendar.Calendar client;

    GoogleClientSecrets clientSecrets;
    GoogleAuthorizationCodeFlow flow;
    Credential credential;

    @Value("${google.client.client-id}")
    private String clientId;
    @Value("${google.client.client-secret}")
    private String clientSecret;
    @Value("${google.client.redirectUri}")
    private String redirectURI;

    private Set<Event> events = new HashSet<>();

    DateTime startDate = new DateTime(System.currentTimeMillis());
    DateTime endDate = new DateTime("2023-05-15T00:00:00.000+05:30");

    public void setEvents(Set<Event> events) {
        this.events = events;
    }

    @RequestMapping(value = "/login/google", method = RequestMethod.GET)
    public RedirectView googleConnectionStatus(HttpServletRequest request) throws Exception {
        return new RedirectView(authorize());
    }

    @RequestMapping(value = "/login/google", method = RequestMethod.GET, params = "code")
    public ResponseEntity<String> oauth2Callback(@RequestParam(value = "code") String code) {

        com.google.api.services.calendar.model.Events eventList;
        String message;
        try {
            TokenResponse response = flow.newTokenRequest(code).setRedirectUri(redirectURI).execute();
            credential = flow.createAndStoreCredential(response, "userID");
            client = new com.google.api.services.calendar.Calendar.Builder(httpTransport, JSON_FACTORY, credential).setApplicationName(APPLICATION_NAME).build();
            Events events = client.events();

            // eventList = events.list("primary").setTimeMin(startDate).setTimeMax(endDate).execute();
            eventList = events.list("mahesh.mane@humancloud.co.in").setTimeMin(startDate).setTimeMax(endDate).execute();

            message = eventList.getItems().toString();
            System.out.println("My:" + eventList.getItems());

        } catch (Exception e) {
            logger.warn("Exception while handling OAuth2 callback (" + e.getMessage() + ")." + " Redirecting to google connection status page.");
            message = "Exception while handling OAuth2 callback (" + e.getMessage() + ")." + " Redirecting to google connection status page.";
        }

        System.out.println("cal message:" + message);
        return new ResponseEntity<>(message, HttpStatus.OK);
    }

    @RequestMapping(value = "/login/google/addEvent", method = RequestMethod.POST, params = "code")
    public ResponseEntity<String> addEvent(@RequestBody EventRequest request, @RequestParam(value = "code") String code) throws MessagingException {

        com.google.api.services.calendar.model.Events eventList;
        String message;
        try {

            TokenResponse response = flow.newTokenRequest(code).setRedirectUri(redirectURI).execute();
            credential = flow.createAndStoreCredential(response, "userID");

            // Build Google Calendar API client
            client = new Calendar.Builder(httpTransport, JSON_FACTORY, credential).setApplicationName(APPLICATION_NAME).build();

            // Build Google Gmail API client
            Gmail gmailService = new Gmail.Builder(httpTransport, JSON_FACTORY, credential).setApplicationName(APPLICATION_NAME).build();

            // Create a new instance of the Event class
            Event event = new Event();

            // Set the values of the Event object using the values from the request body
            event.setSummary(request.getSummary());
            event.setDescription(request.getDescription());
            // event.setLocation(request.getLocation());


              // Set organizer
//            Event.Organizer eventOrganizer = new Event.Organizer();
//            eventOrganizer.setEmail(request.getOrganizerEmail());
//            event.setOrganizer(eventOrganizer);


            // Set start time
            DateTime startDateTime = new DateTime(request.getStart());
            EventDateTime startEventDateTime = new EventDateTime().setDateTime(startDateTime).setTimeZone("Asia/Kolkata");
            event.setStart(startEventDateTime);

            // Set end time
            DateTime endDateTime = new DateTime(request.getEnd());
            EventDateTime endEventDateTime = new EventDateTime().setDateTime(endDateTime).setTimeZone("Asia/Kolkata");
            event.setEnd(endEventDateTime);


            // Set reminders for the event
            EventReminder[] reminderOverrides = new EventReminder[]{
                    new EventReminder().setMethod("email").setMinutes(30)
            };
            Event.Reminders reminders = new Event.Reminders().setUseDefault(false).setOverrides(Arrays.asList(reminderOverrides));
            event.setReminders(reminders);

            // Add attendees
            List<String> attendeeEmails = request.getAttendeeEmail();
            List<EventAttendee> attendees = new ArrayList<>();

            for (String email : attendeeEmails) {
                EventAttendee attendee = new EventAttendee().setEmail(email);
                attendees.add(attendee);
            }
            event.setAttendees(attendees);

            // Set up the conference data
            ConferenceSolutionKey conferenceSolution = new ConferenceSolutionKey().setType("hangoutsMeet");
            CreateConferenceRequest createConferenceRequest = new CreateConferenceRequest().setRequestId(UUID.randomUUID().toString()).setConferenceSolutionKey(conferenceSolution);
            ConferenceData conferenceData = new ConferenceData().setCreateRequest(createConferenceRequest);
            event.setConferenceData(conferenceData);


            // Insert the new event into the calendar
            Event createdEvent = client.events().insert("primary", event).setConferenceDataVersion(1).setSendNotifications(true).setSendUpdates("all").execute();

            System.out.println("Event created: " + createdEvent);

            // Retrieve the Google Meet link from the created event
            String meetLink = createdEvent.getHangoutLink();

            // Send a confirmation email
//            List<String> to = request.getAttendeeEmail();
//            String from = request.getOrganizerEmail();
//            String subject = "Invitation: " + event.getSummary();

            System.out.println("Google Meet Link :" + meetLink);


            // Message message1 = GmailUtil.createEmail(to, from, subject, body);
            // GmailUtil.sendMessage(gmailService, "me", message1);

            return new ResponseEntity<>("Event added successfully.", HttpStatus.OK);

        } catch (IOException e) {
            logger.error("Exception while adding event (" + e.getMessage() + ").");
            return new ResponseEntity<>("Error while adding event.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    public Set<Event> getEvents() throws IOException {
        return this.events;
    }

    private String authorize() throws Exception {
        AuthorizationCodeRequestUrl authorizationUrl;
        if (flow == null) {
            Details web = new Details();
            web.setClientId(clientId);
            web.setClientSecret(clientSecret);
            clientSecrets = new GoogleClientSecrets().setWeb(web);
            httpTransport = GoogleNetHttpTransport.newTrustedTransport();

            Set<String> scopes = new HashSet<>();
            scopes.add(CalendarScopes.CALENDAR);
            scopes.add(GmailScopes.GMAIL_SEND);
            flow = new GoogleAuthorizationCodeFlow.Builder(httpTransport, JSON_FACTORY, clientSecrets, scopes).build();
        }
        authorizationUrl = flow.newAuthorizationUrl().setRedirectUri(redirectURI);
        System.out.println("cal authorizationUrl->" + authorizationUrl);
        return authorizationUrl.build();
    }
}