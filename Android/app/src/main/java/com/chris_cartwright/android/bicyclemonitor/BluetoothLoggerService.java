package com.chris_cartwright.android.bicyclemonitor;

import android.app.IntentService;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.adafruit.bluefruit.le.connect.ble.BleManager;
import com.adafruit.bluefruit.le.connect.ble.KnownUUIDs;

import java.nio.charset.Charset;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.adafruit.bluefruit.le.connect.ble.BleManager.STATE_CONNECTED;

public class BluetoothLoggerService extends IntentService implements BleManager.BleManagerListener {
    private static final String TAG = BluetoothLoggerService.class.getName();

    private final Binder binder = new Binder();

    private BleManager manager;
    private BluetoothGattService uartService;
    private BluetoothDeviceData selected;

    private Pattern sensorData;
    private StringBuilder dataBuffer;
    private DbHelper dbHelper;

    private double speed;
    private int cadence;

    private EventListener listener;

    public double getSpeed() {
        return speed;
    }

    public int getCadence() {
        return cadence;
    }

    public BluetoothDeviceData getSelected() {
        return selected;
    }

    public BluetoothLoggerService() {
        super(BluetoothLoggerService.class.getName());

        dataBuffer = new StringBuilder(10);
        sensorData = Pattern.compile("^S(\\d+)C(\\d+)P(\\d+)");
        dbHelper = new DbHelper(this);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.i(TAG, "Started service");
        manager = BleManager.getInstance(this);
        manager.setBleListener(this);
    }

    private void onData(String data) {
        Log.i(TAG, "Data: " + data);
        Matcher matcher = sensorData.matcher(data);
        if(!matcher.matches()) {
            Log.w(TAG, "Invalid sensor data: " + data);

            if(listener != null) {
                listener.onError("Invalid sensor data received");
            }

            return;
        }

        int speedRpm = Integer.parseInt(matcher.group(1));
        // speed = 2 * Math.PI * 350 * speedRpm * (60/1000000);
        speed = speedRpm * 350 * 0.00037699111843;

        cadence = Integer.parseInt(matcher.group(2));
        int packetNum = Integer.parseInt(matcher.group(3));

        HistoryEntry entry = new HistoryEntry(speed, cadence, packetNum);
        (new DbHelper(this)).add(entry);

        if(listener != null) {
            listener.onData(speed, cadence);
        }
    }

    public void disconnect() {
        if(manager.getState() == STATE_CONNECTED) {
            manager.disconnect();
            selected = null;
        }
    }

    public boolean connect(BluetoothDeviceData data) {
        if(manager.connect(this, data.device.getAddress())){
            selected = data;
            return true;
        }

        return false;
    }

    public boolean isConnected() {
        return manager.getState() == STATE_CONNECTED;
    }

    public void setListener(EventListener listener) {
        this.listener = listener;
    }

    @Override
    public void onConnected() {
        if(listener != null) {
            listener.onConnected();
        }
    }

    @Override
    public void onConnecting() {

    }

    @Override
    public void onDisconnected() {
        Log.i(TAG, "Device disconnected.");
        selected = null;

        if(listener != null) {
            listener.onDisconnected();
        }
    }

    @Override
    public void onServicesDiscovered() {
        Log.i(TAG, "Found services");

        uartService = manager.getGattService(KnownUUIDs.UUID_SERVICE);
        if(uartService == null) {
            selected = null;

            if(listener != null) {
                listener.onError("Required services not found.");
            }

            return;
        }

        manager.enableNotification(uartService, KnownUUIDs.UUID_RX, true);
    }

    @Override
    public synchronized void onDataAvailable(BluetoothGattCharacteristic characteristic) {
        Log.d(TAG, "Data available in characteristic");
        if (!characteristic.getService().getUuid().toString().equalsIgnoreCase(KnownUUIDs.UUID_SERVICE) ||
                !characteristic.getUuid().toString().equalsIgnoreCase(KnownUUIDs.UUID_RX)) {
            return;
        }

        final byte[] bytes = characteristic.getValue();
        final String data = new String(bytes, Charset.forName("UTF-8"));

        dataBuffer.append(data);
        int newline = dataBuffer.indexOf("\n");
        if(newline != -1) {
            String d = dataBuffer.substring(0, newline);
            dataBuffer.delete(0, newline + 1);
            onData(d);
        }
    }

    @Override
    public void onDataAvailable(BluetoothGattDescriptor descriptor) {

    }

    @Override
    public void onReadRemoteRssi(int rssi) {

    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public class Binder extends android.os.Binder {
        BluetoothLoggerService getService() {
            return BluetoothLoggerService.this;
        }
    }

    public interface EventListener {
        void onError(String message);
        void onConnected();
        void onDisconnected();
        void onData(double speed, int cadence);
    }
}
