package eu.binjr.core.controllers;

public enum TimelineDisplayMode {
    DATE_TIME("Dates & times"),
    DURATION("Durations");

    private final String description;

    private TimelineDisplayMode(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public String toString() {
        return this.description;
    }
}
