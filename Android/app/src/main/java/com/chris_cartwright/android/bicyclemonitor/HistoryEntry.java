package com.chris_cartwright.android.bicyclemonitor;

public class HistoryEntry {
    private double speed;
    private int cadence;
    private int packetNum;

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

    public int getPacketNum() {
        return packetNum;
    }

    public void setPacketNum(int packetNum) {
        this.packetNum = packetNum;
    }

    public HistoryEntry(double speed, int cadence, int packetNum) {
        this.speed = speed;
        this.cadence = cadence;
        this.packetNum = packetNum;
    }
}