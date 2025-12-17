package com.ergi.rusdihari.models;

public class Event {
    private String eventName;
    private String eventDate;

    public Event(String eventName, String eventDate) {
        this.eventName = eventName;
        this.eventDate = eventDate;
    }

    public String getEventName() {
        return eventName;
    }

    public String getEventDate() {
        return eventDate;
    }
}
