package com.chris_cartwright.android.bicyclemonitor;

public class HistoryEntry {
    private double speed;
    private int cadence;

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

    public HistoryEntry(double speed, int cadence) {
        this.speed = speed;
        this.cadence = cadence;
    }
}