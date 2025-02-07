package com.example.simpleservercpp;
import android.Manifest;
import android.bluetooth.*;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.ParcelUuid;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class BLEServer {
    private static final String TAG = "BLEServer";
    private static final UUID SERVICE_UUID = UUID.fromString("0000180F-0000-1000-8000-00805F9B34FB");
    //    private static final UUID SERVICE_UUID = UUID.fromString("a7bb1500-eef2-4a8e-80d4-13a83c8cf46f");
    private static final UUID CHARACTERISTIC_UUID = UUID.fromString("a7bb1501-eef2-4a8e-80d4-13a83c8cf46f");

    // Used to load the 'simpleservercpp' library on application startup.
    static {
        System.loadLibrary("simpleservercpp");
    }


    private final BluetoothManager bluetoothManager;
    private final BluetoothAdapter bluetoothAdapter;
    private BluetoothGattServer gattServer;
    private BluetoothGattCharacteristic characteristic;
    private final BluetoothLeAdvertiser advertiser;
    private final Context context;
    private final TextView connectionStatus;
    private final TextView clientState;
    private final EditText receivedDataField;
    private final EditText inputField;
    private final Button sendButton;
    private final Button advertisingButton;
    private volatile boolean isAdvertising = false;
    private final Handler handler;

    public BLEServer(Context context, TextView connectionStatus, TextView clientState, EditText receivedDataField, EditText inputField, Button sendButton, Button advertisingButton) {
        this.context = context;
        this.connectionStatus = connectionStatus;
        this.clientState = clientState;
        this.receivedDataField = receivedDataField;
        this.inputField = inputField;
        this.sendButton = sendButton;
        this.advertisingButton = advertisingButton;
        this.handler = new Handler();
        context = context;
        bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
        }
        bluetoothAdapter.setName("valeo");
        advertiser = bluetoothAdapter.getBluetoothLeAdvertiser();

        if (!bluetoothAdapter.isMultipleAdvertisementSupported()) {
            Log.e(TAG, "BLE advertising is not supported on this device.");
            updateConnectionStatus("BLE advertising not supported");
            advertisingButton.setEnabled(false);
            return;
        }

        requestPermissions();
        startServer();

        setupUI();
        initUdp();
    }

    private void requestPermissions() {
        String[] permissions = {
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_ADVERTISE,
                Manifest.permission.ACCESS_FINE_LOCATION
        };

        if (!checkPermissions()) {
            if (context instanceof android.app.Activity) {
                ActivityCompat.requestPermissions((android.app.Activity) context, permissions, 1);
            } else {
                Log.e(TAG, "Context is not an Activity. Cannot request permissions.");
            }
        } else {
            Log.d(TAG, "All permissions already granted.");
        }
    }

    private boolean checkPermissions() {
        String[] permissions = {
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_ADVERTISE,
                Manifest.permission.ACCESS_FINE_LOCATION
        };

        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private void startServer() {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        gattServer = bluetoothManager.openGattServer(context, new BluetoothGattServerCallback() {
            @Override
            public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
                super.onConnectionStateChange(device, status, newState);
                {
                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        Log.d(TAG, "Device connected: " + (device != null ? device.getAddress() : "Unknown"));
                        updateClientState("Connected");
                    } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        Log.d(TAG, "Device disconnected");
                        updateClientState("Disconnected");
                    }
                }

            }

            private void disconnectDevice(BluetoothDevice device) {
                try {
                    if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                    device.connectGatt(context, false, new BluetoothGattCallback() {
                        @Override
                        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                            if (newState == BluetoothProfile.STATE_CONNECTED) {
                                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                                    return;
                                }
                                gatt.disconnect();
                            }
                        }
                    });
                } catch (Exception e) {
                    Log.e("BLE_Server", "Error disconnecting unauthorized device", e);
                }
            }

            @Override
            public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
                super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite, responseNeeded, offset, value);
                if (characteristic.getUuid().equals(CHARACTERISTIC_UUID)) {
                    receiveData(value);
                    if (responseNeeded) {
                        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                            return;
                        }
                        gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, null);
                    }
                }
            }
        });

        if (gattServer == null) {
            Log.e(TAG, "Failed to create GATT server.");
            updateConnectionStatus("Failed to create GATT server");
            return;
        }

        BluetoothGattService service = new BluetoothGattService(SERVICE_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY);
        characteristic = new BluetoothGattCharacteristic(
                CHARACTERISTIC_UUID,
                BluetoothGattCharacteristic.PROPERTY_WRITE | BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PERMISSION_WRITE | BluetoothGattCharacteristic.PERMISSION_READ
        );
        service.addCharacteristic(characteristic);
        gattServer.addService(service);
    }

    private void setupUI() {
        sendButton.setOnClickListener(v -> {
            String dataToSend = inputField.getText().toString();
            if (!dataToSend.isEmpty()) {
                sendData(dataToSend);
            } else {
                Log.e(TAG, "Input field is empty.");
            }
        });

        advertisingButton.setOnClickListener(v -> {
            if (isAdvertising) {
                stopAdvertising();
            } else {
                startAdvertising();
            }
        });
    }

    private void startAdvertising() {
        AdvertiseSettings settings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
                .setConnectable(true)
                .build();

        AdvertiseData data = new AdvertiseData.Builder()
                .setIncludeDeviceName(true)
                .addServiceUuid(new ParcelUuid(SERVICE_UUID))
                .build();

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADVERTISE) != PackageManager.PERMISSION_GRANTED) {

            return;
        }
        advertiser.startAdvertising(settings, data, new AdvertiseCallback() {
            @Override
            public void onStartSuccess(AdvertiseSettings settingsInEffect) {
                super.onStartSuccess(settingsInEffect);
                Log.d(TAG, "Advertising started successfully.");
                updateConnectionStatus("Advertising started");
                isAdvertising = true;
                advertisingButton.setText("Stop Advertising");
            }

            @Override
            public void onStartFailure(int errorCode) {
                super.onStartFailure(errorCode);
                Log.e(TAG, "Advertising failed with error code: " + errorCode);
                updateConnectionStatus("Advertising failed");
                isAdvertising = false;
                advertisingButton.setText("Start Advertising");
            }
        });
    }

    private void stopAdvertising() {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADVERTISE) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        advertiser.stopAdvertising(new AdvertiseCallback() {
            @Override
            public void onStartSuccess(AdvertiseSettings settingsInEffect) {
                Log.d(TAG, "Advertising stopped successfully.");
                updateConnectionStatus("Advertising stopped");
                isAdvertising = false;
                advertisingButton.setText("Start Advertising");
            }

            @Override
            public void onStartFailure(int errorCode) {
                Log.e(TAG, "Failed to stop advertising with error code: " + errorCode);
                updateConnectionStatus("Failed to stop advertising");
            }
        });
    }

    public void sendData(String data) {
        if (characteristic == null || gattServer == null) {
            Log.e(TAG, "Characteristic or GattServer is not initialized.");
            return;
        }

        characteristic.setValue(data.getBytes(StandardCharsets.UTF_8));
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        for (BluetoothDevice device : bluetoothManager.getConnectedDevices(BluetoothProfile.GATT)) {
            gattServer.notifyCharacteristicChanged(device, characteristic, false);
            Log.d(TAG, "Sent data to " + device.getAddress() + ": " + data);
        }
    }

    public void receiveData(byte[] value) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : value) {
            hexString.append(String.format("%02X ", b)); // Converts byte to two-digit hex
        }

        Log.d(TAG, "Received data (bytes): " + hexString.toString().trim());

        receivedDataField.post(() -> receivedDataField.setText(hexString.toString().trim()));

        //this function should be called in Ble receiption call back
        sendOverUdp(value);
    }

    private void updateConnectionStatus(String status) {
        if (connectionStatus != null) {
            connectionStatus.post(() -> connectionStatus.setText(status));
        }
    }

    private void updateClientState(String state) {
        if (clientState != null) {
            clientState.post(() -> clientState.setText("Client State: " + state));
        }
    }



    public native void sendOverUdp(byte[] args);
    public native void initUdp();
    public native void deInitUdp();
}


