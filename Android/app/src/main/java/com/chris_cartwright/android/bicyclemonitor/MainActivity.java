package com.chris_cartwright.android.bicyclemonitor;

import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.*;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import com.adafruit.bluefruit.le.connect.ble.BleDevicesScanner;
import com.adafruit.bluefruit.le.connect.ble.BleManager;
import com.adafruit.bluefruit.le.connect.ble.BleManager.BleManagerListener;
import com.adafruit.bluefruit.le.connect.ble.BleUtils;
import com.adafruit.bluefruit.le.connect.ble.KnownUUIDs;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity implements BleManagerListener {
    private static final String TAG = MainActivity.class.getName();
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;

    private TextView cadenceTextView;
    private TextView speedTextView;
    private TextView statusTextView;

    private int cadence;
    private double speed;
    private String status;

    private Pattern sensorData;

    private BleManager manager;
    private BleDevicesScanner scanner;
    private BluetoothGattService uartService;

    private ArrayList<BluetoothDeviceData> scannedDevices;
    private BluetoothDeviceData selectedDeviceData;
    private DeviceListAdaptor deviceListAdaptor;
    private StringBuilder dataBuffer;

    // Needs to run on UI thread because BleDevicesScanner creates a Handler
    private void startScan() {
        Log.d(TAG, "startScan");

        status = "Scanning...";
        updateUI();

        if(manager.getState() == BleManager.STATE_CONNECTED) {
            manager.disconnect();
            selectedDeviceData = null;
        }

        stopScanning();

        // Configure scanning
        BluetoothAdapter bluetoothAdapter = BleUtils.getBluetoothAdapter(getApplicationContext());
        if (BleUtils.getBleStatus(this) != BleUtils.STATUS_BLE_ENABLED) {
            Log.w(TAG, "startScan: BluetoothAdapter not initialized or unspecified address.");
        } else {
            scanner = new BleDevicesScanner(bluetoothAdapter, null, new BluetoothAdapter.LeScanCallback() {
                private long lastMillis;

                @Override
                public void onLeScan(final BluetoothDevice device, final int rssi, byte[] scanRecord) {
                    final String deviceName = device.getName();

                    BluetoothDeviceData previouslyScannedDeviceData = null;

                    // Check that the device was not previously found
                    for (BluetoothDeviceData deviceData : scannedDevices) {
                        if (deviceData.device.getAddress().equals(device.getAddress())) {
                            previouslyScannedDeviceData = deviceData;
                            break;
                        }
                    }

                    BluetoothDeviceData deviceData;
                    if (previouslyScannedDeviceData == null) {
                        // Add it to the mScannedDevice list
                        deviceData = new BluetoothDeviceData();
                        scannedDevices.add(deviceData);
                    } else {
                        deviceData = previouslyScannedDeviceData;
                    }

                    deviceData.device = device;
                    deviceData.rssi = rssi;
                    deviceData.scanRecord = scanRecord;
                    deviceData.decodeScanRecords();

                    // Update device data
                    // Avoid updating when not a new device has been found and the time from the last update is really
                    // short to avoid updating UI so fast that it will become unresponsive
                    long currentMillis = SystemClock.uptimeMillis();
                    if (previouslyScannedDeviceData == null || currentMillis - lastMillis > 500) {
                        lastMillis = currentMillis;
                        updateUI();
                    }
                }
            });

            // Start scanning
            scanner.start();
        }
    }

    private void stopScanning() {
        if (scanner == null) {
            return;
        }

        scanner.stop();
        scanner = null;
    }

    private void updateUI() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                deviceListAdaptor.notifyDataSetChanged();
                statusTextView.setText(status);
                cadenceTextView.setText(cadence + "");
                speedTextView.setText(String.format("%.1f", speed));
            }
        });
    }

    private void onData(String data) {
        Log.i(TAG, "Data: " + data);
        Matcher matcher = sensorData.matcher(data);
        if(!matcher.matches()) {
            Toast.makeText(MainActivity.this, "Failed to parse sensor data.", Toast.LENGTH_LONG);
            Log.w(TAG, "Invalid sensor data: " + data);
            return;
        }

        int speedRpm = Integer.parseInt(matcher.group(1));
        // speed = 2 * Math.PI * 350 * speedRpm * (60/1000000);
        speed = speedRpm * 350 * 0.00037699111843;

        cadence = Integer.parseInt(matcher.group(2));

        updateUI();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        scannedDevices = new ArrayList<>();
        dataBuffer = new StringBuilder(10);
        sensorData = Pattern.compile("S(\\d+)C(\\d+)");

        // Grab references to UI elements.
        speedTextView = (TextView) findViewById(R.id.tvSpeed);
        cadenceTextView = (TextView) findViewById(R.id.tvCadence);
        statusTextView = (TextView) findViewById(R.id.tvStatus);

        manager = BleManager.getInstance(this);
        manager.setBleListener(this);

        deviceListAdaptor = new DeviceListAdaptor(MainActivity.this, scannedDevices);
        ((ListView)findViewById(R.id.lvDevices)).setAdapter(deviceListAdaptor);

        status = "Checking permissions";
        updateUI();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android M permission check
            if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("This app needs location access");
                builder.setMessage("Please grant location access to this app can detect BLE devices.");
                builder.setPositiveButton(android.R.string.ok, null);
                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
                    }
                });
                builder.show();
            } else {
                startScan();
            }
        } else {
            startScan();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        manager.setBleListener(this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_COARSE_LOCATION: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "coarse location permission granted");
                    startScan();
                } else {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Functionality limited");
                    builder.setMessage("Since location access has not been granted, this app will not be able to discover beacons when in the background.");
                    builder.setPositiveButton(android.R.string.ok, null);
                    builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                            status = "No permission access.";
                            updateUI();
                        }
                    });
                    builder.show();
                }
            }
        }
    }

    @Override
    public void onConnected() {
        Log.i(TAG, "Connected to device: " + selectedDeviceData.getNiceName());

        stopScanning();

        status = "Connected to " + selectedDeviceData.getNiceName();
        scannedDevices.clear();

        updateUI();
    }

    @Override
    public void onConnecting() {
    }

    @Override
    public void onDisconnected() {
        Log.i(TAG, "Device disconnected.");
        selectedDeviceData = null;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                startScan();
            }
        });
    }

    @Override
    public void onServicesDiscovered() {
        Log.i(TAG, "Found services");

        uartService = manager.getGattService(KnownUUIDs.UUID_SERVICE);
        if(uartService == null) {
            selectedDeviceData = null;

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(MainActivity.this, "Failed to connect to BLE device", Toast.LENGTH_LONG);
                    startScan();
                }
            });

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
    public synchronized void onDataAvailable(BluetoothGattDescriptor descriptor) {
    }

    @Override
    public void onReadRemoteRssi(int rssi) {
    }

    public class DeviceListAdaptor extends ArrayAdapter<BluetoothDeviceData> {
        private final Context context;
        private final ArrayList<BluetoothDeviceData> values;

        public DeviceListAdaptor(Context context, ArrayList<BluetoothDeviceData> values) {
            super(context, R.layout.listiew_device, values);
            this.context = context;
            this.values = values;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View rowView = inflater.inflate(R.layout.listiew_device, parent, false);
            TextView textView = (TextView) rowView.findViewById(R.id.lblName);
            Button button = (Button) rowView.findViewById(R.id.btnConnect);

            final BluetoothDeviceData data = values.get(position);

            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.i(TAG, "Connect to " + data.getNiceName());
                    if(manager.connect(DeviceListAdaptor.this.context, data.device.getAddress())){
                        selectedDeviceData = data;
                    }
                }
            });
            textView.setText(data.getNiceName());

            return rowView;
        }
    }
}
