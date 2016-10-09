package com.chris_cartwright.android.bicyclemonitor;

import java.util.Date;
import java.util.UUID;

public class HistoryEntry {
    private double speed;
    private int cadence;
    private UUID uuid;
    private Date created;

    public double getSpeed() {
        return speed;
    }

    public void setSpeed(double speed) {
        this.speed = speed;
    }

    public int getCadence() {
        return cadence;
    }

    public void setCadence(int cadence) {
        this.cadence = cadence;
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public HistoryEntry(){}

    public HistoryEntry(double speed, int cadence) {
        this.uuid = UUID.randomUUID();
        this.speed = speed;
        this.cadence = cadence;
    }
}