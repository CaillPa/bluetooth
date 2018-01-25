package com.example.bluetooth;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static java.lang.Thread.sleep;

public class MainActivity extends AppCompatActivity {

    private final int SCAN_DURATION_SECONDS = 12;
    private final int DISCOVERABLE_DURATION_SECONDS = 30;
    private final int BT_DISCOVER = 1;
    private final int BT_ENABLE_SCAN = 2;

    ToggleButton btnBT;
    Button btnDsc;
    Button btnScan;
    ProgressBar pb;
    RecyclerView recyclerView;

    BluetoothAdapter bluetoothAdapter;
    List<BluetoothDevice> btDevices;
    Set<BluetoothDevice> pairedDevices;
    RecyclerDevicesAdapter adapter;

    BroadcastReceiver brOnOff;
    BroadcastReceiver brDsc;
    BroadcastReceiver brFound;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /* ****************************************
            findViewById
         */
        btnBT = findViewById(R.id.btnBT);
        btnDsc = findViewById(R.id.btnDsc);
        btnScan = findViewById(R.id.btnScan);
        pb = findViewById(R.id.pb);
        recyclerView = findViewById(R.id.recyclerView);

        btDevices = new ArrayList<>();
        pb.setVisibility(View.INVISIBLE);
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(bluetoothAdapter == null) {
            // Device doesn't support Bluetooth
            Toast.makeText(this, "Your device is not compatible with Bluetooth!", Toast.LENGTH_SHORT).show();
            return;
        }
        pairedDevices = bluetoothAdapter.getBondedDevices();
        btDevices.addAll(pairedDevices);


        /* ****************************************
            RecyclerView
         */
        recyclerView.setHasFixedSize(true); // for performance
        recyclerView.setLayoutManager(new LinearLayoutManager(this));   // vertical scrolling
        adapter = new RecyclerDevicesAdapter(btDevices, R.layout.bluetooth_device, getApplicationContext());
        recyclerView.setAdapter(adapter);

        // touchListeners for the RecyclerView
        recyclerView.addOnItemTouchListener(
            new RecyclerTouchListener(getApplicationContext(), recyclerView,
                new RecyclerTouchListener.OnItemClickListener() {
                    @Override
                    public void onClick(View view, int position) {
                        // request for pairing with the selected device
                        adapter.get(position).createBond();
                    }

                    @Override
                    public void onLongClick(View view, int position) {
                        switchTransfertActivity(adapter.get(position));
                    }
                }
            )
        );

        /* ****************************************
            BroadcastReceiver
         */

        // broadcast receiver to update UI if the user toggles BT outside of the app
        brOnOff = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                updateBTBtnTxt();
            }
        };
        registerReceiver(brOnOff, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));

        // broadcast receiver to update the discoverable UI button
        brDsc = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                updateDscBtnTxt();
            }
        };
        registerReceiver(brDsc, new IntentFilter(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED));

        // broadcast receiver to update the RecyclerView when a device is found
        brFound = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                btDevices.add(device);
                adapter.notifyDataSetChanged();

                Toast.makeText(context, String.format("found : %s", device.getName()), Toast.LENGTH_SHORT).show();
            }
        };
        registerReceiver(brFound, new IntentFilter(BluetoothDevice.ACTION_FOUND));

        /* ****************************************
            Button click listeners
         */

        btnBT.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(bluetoothAdapter.isEnabled()) {
                    bluetoothAdapter.disable();
                } else {
                    bluetoothAdapter.enable();
                }
            }
        });

        btnDsc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, DISCOVERABLE_DURATION_SECONDS);
                startActivityForResult(discoverableIntent, BT_DISCOVER);
            }
        });

        btnScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                if(!bluetoothAdapter.isEnabled()) {
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, BT_ENABLE_SCAN);
                } else {
                    startScan(SCAN_DURATION_SECONDS);
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == BT_ENABLE_SCAN) {
            // starts a scan if the user enabled bluetooth after accepting to enable bluetooth
            if(resultCode == Activity.RESULT_OK) {
                startScan(SCAN_DURATION_SECONDS);
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        // update button state so it matches devices' state
        updateBTBtnTxt();
        updateDscBtnTxt();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // we have to unregister the broadcast receivers
        unregisterReceiver(brOnOff);
        unregisterReceiver(brDsc);
        unregisterReceiver(brFound);
    }

    private void switchTransfertActivity(BluetoothDevice bt) {
        Intent intent = new Intent(this, PhotoActivity.class);
        intent.putExtra("device", bt);
        startActivity(intent);
    }

    private void startScan(final int seconds) {
        final Handler handler = new Handler();
        pb.setVisibility(View.VISIBLE);
        btDevices.clear();
        adapter.notifyDataSetChanged();
        btnScan.setEnabled(false);
        Runnable runnable = new Runnable() {
            public void run() {
                bluetoothAdapter.startDiscovery();
                try {
                    sleep(seconds*1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    bluetoothAdapter.cancelDiscovery();
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            pb.setVisibility(View.INVISIBLE);
                            btnScan.setEnabled(true);
                        }
                    });
                }
            }
        };
        new Thread(runnable).start();
    }

    private void updateBTBtnTxt() {
        if(bluetoothAdapter.isEnabled()) {
            btnBT.setChecked(true);
        } else {
            btnBT.setChecked(false);
        }
    }

    private void updateDscBtnTxt() {
        if(bluetoothAdapter.getScanMode() == BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            btnDsc.setText("Visible");
        } else {
            btnDsc.setText("Invisible");
        }
    }
}
