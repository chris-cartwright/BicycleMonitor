package com.chris_cartwright.android.bicyclemonitor;

public class HistoryEntry {
    private int speed;
    private int cadence;
    private int packetNum;

    /**
     * In RPM
     * @return
     */
    public int getSpeed() {
        return speed;
    }

    /**
     * In RPM
     * @param speed
     */
    public void setSpeed(int speed) {
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

    public HistoryEntry(int speed, int cadence, int packetNum) {
        this.speed = speed;
        this.cadence = cadence;
        this.packetNum = packetNum;
    }
}