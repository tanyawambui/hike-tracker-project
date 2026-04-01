package com.hiketracker.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "hikes")
public class Hike {
    @PrimaryKey(autoGenerate = true)
    private int id;

    private String userId;
    private String date;           // "2024-03-15"
    private long startTimeMillis;
    private long endTimeMillis;
    private float distanceMeters;  // total distance in meters
    private long durationSeconds;  // total duration

    // Constructors
    public Hike() {}

    public Hike(String userId, String date, long startTimeMillis, long endTimeMillis,
                float distanceMeters, long durationSeconds) {
        this.userId = userId;
        this.date = date;
        this.startTimeMillis = startTimeMillis;
        this.endTimeMillis = endTimeMillis;
        this.distanceMeters = distanceMeters;
        this.durationSeconds = durationSeconds;
    }

    // Getters & Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public long getStartTimeMillis() { return startTimeMillis; }
    public void setStartTimeMillis(long startTimeMillis) { this.startTimeMillis = startTimeMillis; }

    public long getEndTimeMillis() { return endTimeMillis; }
    public void setEndTimeMillis(long endTimeMillis) { this.endTimeMillis = endTimeMillis; }

    public float getDistanceMeters() { return distanceMeters; }
    public void setDistanceMeters(float distanceMeters) { this.distanceMeters = distanceMeters; }

    public long getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(long durationSeconds) { this.durationSeconds = durationSeconds; }

    // Helper: distance in km formatted
    public String getFormattedDistance() {
        if (distanceMeters >= 1000) {
            return String.format("%.2f km", distanceMeters / 1000f);
        } else {
            return String.format("%.0f m", distanceMeters);
        }
    }

    // Helper: duration formatted as HH:MM:SS
    public String getFormattedDuration() {
        long hours = durationSeconds / 3600;
        long minutes = (durationSeconds % 3600) / 60;
        long seconds = durationSeconds % 60;
        if (hours > 0) {
            return String.format("%02d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format("%02d:%02d", minutes, seconds);
        }
    }
}
