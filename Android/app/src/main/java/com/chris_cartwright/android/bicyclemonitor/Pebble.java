package com.chris_cartwright.android.bicyclemonitor;

import android.content.Context;

import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;

import java.util.UUID;

public class Pebble {
    public static final UUID APP_UUID = UUID.fromString("2e24a1a9-021d-4ad3-a7db-d8249d9de6de");
    public static final int KEY_SPEED = 10000;
    public static final int KEY_CADENCE = 10001;
    public static final int KEY_VIBRATE = 10002;
    public static final byte VIBRATE_CADENCE_LOW = 1;
    public static final byte VIBRATE_CADENCE_HIGH = 2;
    public static final byte VIBRATE_CADENCE_GOOD = 3;

    private final Context context;

    public Pebble(Context context) {
        this.context = context;
    }

    public void startApp() {
        PebbleKit.startAppOnPebble(context, APP_UUID);
    }

    public void vibrate(byte pattern) {
        PebbleDictionary dict = new PebbleDictionary();
        dict.addInt8(KEY_VIBRATE, pattern);
        PebbleKit.sendDataToPebble(context, APP_UUID, dict);
    }

    public void updateStats(double speed, int cadence) {
        PebbleDictionary dict = new PebbleDictionary();
        dict.addString(KEY_SPEED, String.format("%.1f", speed));
        dict.addInt32(KEY_CADENCE, cadence);
        PebbleKit.sendDataToPebble(context, APP_UUID, dict);
    }
}
