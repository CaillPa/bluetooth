package com.example.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

import java.io.IOException;
import java.util.UUID;

/**
 * Created by paul on 23/01/18.
 */

public class ConnectThread extends Thread {
    private final BluetoothSocket bluetoothSocket;

    public ConnectThread(BluetoothDevice device) {
        BluetoothSocket tmp = null;
        try {
            tmp = device.createRfcommSocketToServiceRecord(UUID.fromString("33b61ba2-004b-11e8-ba89-0ed5f89f718b"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        bluetoothSocket = tmp;
    }

    public void run() {
        try {
            bluetoothSocket.connect();
        } catch (IOException e) {
            e.printStackTrace();
            try {
                bluetoothSocket.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            return;
        }
        // work here
    }

    public void cancel() {
        try {
            bluetoothSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
