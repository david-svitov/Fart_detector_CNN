package com.example.fartdetector;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.ParcelUuid;
import android.util.Log;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Set;

public class HCConnector {

    private BluetoothAdapter bluetoothAdapter;
    private OutputStream outputStream = null;

    public HCConnector() throws Exception {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (!bluetoothAdapter.isEnabled()) {
            throw new Exception("Bluetooth is disabled");
        }

        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();

        if (pairedDevices.size() <= 0) {
            throw new Exception("No bluetooth devices found");
        }

        for (BluetoothDevice device : pairedDevices) {
            String deviceName = device.getName();
            if (deviceName.equals("HC-06")) {
                ParcelUuid[] uuids = device.getUuids();
                BluetoothSocket socket = device.createRfcommSocketToServiceRecord(uuids[0].getUuid());
                socket.connect();
                outputStream = socket.getOutputStream();
            }
        }

        if (outputStream == null) {
            throw new Exception("HC-06 device is not found");
        }
    }

    public void sendValue(String value) throws IOException {
        outputStream.write(value.getBytes());
    }
}
