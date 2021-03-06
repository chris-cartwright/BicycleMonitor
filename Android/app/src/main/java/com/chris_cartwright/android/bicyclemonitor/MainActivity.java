package com.chris_cartwright.android.bicyclemonitor;

import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.*;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.LocationManager;
import android.os.Build;
import android.os.IBinder;
import android.os.SystemClock;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import com.adafruit.bluefruit.le.connect.ble.BleDevicesScanner;
import com.adafruit.bluefruit.le.connect.ble.BleUtils;
import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;

import org.acra.ACRA;

import java.util.ArrayList;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements BluetoothLoggerService.EventListener {
    private static final String TAG = MainActivity.class.getName();
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;

    private TextView cadenceTextView;
    private TextView speedTextView;
    private TextView statusTextView;

    private int cadence;
    private int speedRpm;
    private double speedKmh;
    private String status;

    private BleDevicesScanner scanner;

    private ArrayList<BluetoothDeviceData> scannedDevices;
    private DeviceListAdaptor deviceListAdaptor;
    private BluetoothLoggerService bluetoothService;

    private Pebble pebble;
    private CadenceWatcher cadenceWatcher;

    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            BluetoothLoggerService.Binder binder = (BluetoothLoggerService.Binder) service;
            bluetoothService = binder.getService();
            bluetoothService.setListener(MainActivity.this);
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            bluetoothService = null;
        }
    };

    // Needs to run on UI thread because BleDevicesScanner creates a Handler
    private void startScan() {
        Log.d(TAG, "startScan");

        status = "Scanning...";
        updateUI();

        if(bluetoothService != null) {
            bluetoothService.disconnect();
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

        scannedDevices.clear();
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
                speedTextView.setText(String.format("%.1f", speedKmh));

                findViewById(R.id.lvDevices).setVisibility(scanner == null ? View.INVISIBLE : View.VISIBLE);
            }
        });
    }

    private void verifyGps() {
        // http://stackoverflow.com/a/3470757
        LocationManager locationManager = (LocationManager)MainActivity.this.getSystemService(Context.LOCATION_SERVICE);

        Criteria locationCriteria = new Criteria();
        locationCriteria.setAccuracy(Criteria.ACCURACY_COARSE);
        locationCriteria.setAltitudeRequired(false);
        locationCriteria.setBearingRequired(false);
        locationCriteria.setCostAllowed(true);
        locationCriteria.setPowerRequirement(Criteria.NO_REQUIREMENT);

        String providerName = locationManager.getBestProvider(locationCriteria, true);

        if (providerName == null || !locationManager.isProviderEnabled(providerName)) {
            Toast.makeText(MainActivity.this, "Please enable location services", Toast.LENGTH_LONG).show();
            Intent myIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            MainActivity.this.startActivity(myIntent);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        scannedDevices = new ArrayList<>();

        Intent i = new Intent(this, BluetoothLoggerService.class);
        startService(i);

        // Grab references to UI elements.
        speedTextView = (TextView) findViewById(R.id.tvSpeed);
        cadenceTextView = (TextView) findViewById(R.id.tvCadence);
        statusTextView = (TextView) findViewById(R.id.tvStatus);

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
                verifyGps();
                startScan();
            }
        } else {
            verifyGps();
            startScan();
        }

        pebble = new Pebble(getApplicationContext());
        cadenceWatcher = new CadenceWatcher();
    }

    @Override
    public void onResume() {
        super.onResume();

        pebble.startApp();
    }

    @Override
    public void onStart() {
        super.onStart();

        Intent i = new Intent(this, BluetoothLoggerService.class);
        bindService(i, connection, Context.BIND_AUTO_CREATE);

        pebble.startApp();
    }

    @Override
    public void onStop() {
        super.onStop();

        if(bluetoothService != null) {
            unbindService(connection);
            bluetoothService = null;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        stopService(new Intent(this, BluetoothLoggerService.class));
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
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(Menu.NONE, 1, Menu.NONE, "Send crash report");
        menu.add(Menu.NONE, 2, Menu.NONE, "Test vibrate low");
        menu.add(Menu.NONE, 3, Menu.NONE, "Test vibrate high");
        menu.add(Menu.NONE, 4, Menu.NONE, "Test vibrate good");
        menu.add(Menu.NONE, 5, Menu.NONE, "Disconnect");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case 1:
                ACRA.getErrorReporter().handleException(null);
                return true;

            case 2:
                pebble.vibrate(Pebble.VIBRATE_CADENCE_LOW);
                return true;

            case 3:
                pebble.vibrate(Pebble.VIBRATE_CADENCE_HIGH);
                return true;

            case 4:
                pebble.vibrate(Pebble.VIBRATE_CADENCE_GOOD);
                return true;

            case 5:
                startScan();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onError(String message) {
        Log.e(TAG, message);
        final String msg = message;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, msg, Toast.LENGTH_LONG);
            }
        });
    }

    @Override
    public void onConnected() {
        stopScanning();

        status = "Connected to " + bluetoothService.getSelected().advertisedName;
        updateUI();
    }

    @Override
    public void onDisconnected() {
        startScan();
    }

    @Override
    public void onData(int speed, int cadence) {
        this.speedRpm = speed;
        this.cadence = cadence;

        // (2 * PI * r * rpm * 60) / 1000
        this.speedKmh = (2 * 3.1416 * 0.335 * speed * 60) / 1000;

        updateUI();
        pebble.updateStats(this.speedKmh, cadence);
        cadenceWatcher.update(cadence);
    }

    private class CadenceWatcher {
        private static final int LOW = 80;
        private static final int HIGH = 100;

        // Start off as too slow because the bike isn't moving
        // -1 Slow
        // 0 Good
        // 1 High
        private int last = -1;

        public void update(int cadence) {
            int newState;

            if(cadence < LOW) {
                newState = -1;
            }
            else if(cadence > HIGH) {
                newState = 1;
            }
            else {
                newState = 0;
            }

            // Only vibrate if our state has changed
            if(newState == last) {
                return;
            }

            last = newState;

            if(newState == -1) {
                MainActivity.this.pebble.vibrate(Pebble.VIBRATE_CADENCE_LOW);
            }
            else if(newState == 0) {
                MainActivity.this.pebble.vibrate(Pebble.VIBRATE_CADENCE_GOOD);
            }
            else {
                MainActivity.this.pebble.vibrate(Pebble.VIBRATE_CADENCE_HIGH);
            }
        }
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
                    bluetoothService.connect(data);

                }
            });
            textView.setText(data.getNiceName());

            return rowView;
        }
    }
}
