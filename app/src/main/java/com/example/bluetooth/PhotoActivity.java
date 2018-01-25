package com.example.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

public class PhotoActivity extends AppCompatActivity {

    static final int REQUEST_IMAGE_CAPTURE = 1;

    private ToggleButton tglBtn;
    private ImageButton btnPhoto;
    private Button btnEnvoyer;
    private ImageView img;
    private TextView txtPaired;

    private Bitmap bmpImage;
    private Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo);

        /* ****************************************
            findViewById
         */
        tglBtn = findViewById(R.id.tglBtn);
        btnPhoto = findViewById(R.id.btnPhoto);
        btnEnvoyer = findViewById(R.id.btnEnvoyer);
        img = findViewById(R.id.img);
        txtPaired = findViewById(R.id.txtPaired);

        // get selected BT device
        Intent intent = getIntent();
        final BluetoothDevice device = intent.getParcelableExtra("device");
        txtPaired.setText(String.format(getString(R.string.txt_photo_paired), device.getName(), device.getAddress()));
        final BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        handler = new Handler();

        // checkButton setup
        tglBtn.setChecked(false);
        checkReceiverMode();

        /* ****************************************
            clickListeners
         */
        tglBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                checkReceiverMode();
                if(tglBtn.isChecked()) {
                    new AcceptThread(bluetoothAdapter).start();
                } else {
                    new AcceptThread(bluetoothAdapter).cancel();
                }
            }
        });

        btnPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                    startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
                }
            }
        });

        btnEnvoyer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new ConnectThread(device, bmpImage).start();
            }
        });
    }

    private void checkReceiverMode() {
        // enable/disable buttons
        btnEnvoyer.setEnabled(!tglBtn.isChecked());
        btnPhoto.setEnabled(!tglBtn.isChecked());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // update the BMP image when coming back from the cmaera app
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");
            this.bmpImage = imageBitmap;
            img.setImageBitmap(imageBitmap);
        }
    }

    private void sendImage(Bitmap image, BluetoothSocket socket) throws IOException {
        // write the BMP image to the socket's buffer
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.PNG, 100, stream);
        byte[] byteArray = stream.toByteArray();
        socket.getOutputStream().write(byteArray);
    }

    private Bitmap receiveImage(BluetoothSocket socket) throws IOException {
        InputStream stream = socket.getInputStream();
        Bitmap res = BitmapFactory.decodeStream(stream);
        stream.close();
        return res;
    }

    private void changeImage(final Bitmap image) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                img.setImageBitmap(image);
            }
        });
    }

    private class ConnectThread extends Thread {
        private final BluetoothSocket bluetoothSocket;
        private final Bitmap image;

        public ConnectThread(BluetoothDevice device, Bitmap image) {
            this.image = image;
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

            try {
                sendImage(this.image, bluetoothSocket);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void cancel() {
            try {
                bluetoothSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private class AcceptThread extends Thread {
        private final BluetoothAdapter bluetoothAdapter;
        private final BluetoothServerSocket bluetoothServerSocket;

        public AcceptThread(BluetoothAdapter ba) {
            this.bluetoothAdapter = ba;
            // temporary object because serverSocket is final
            BluetoothServerSocket tmp = null;
            try {
                // MY_UUID is the app's UUID string, also used by the client code.
                tmp = bluetoothAdapter.listenUsingRfcommWithServiceRecord("Recepteur", UUID.fromString("33b61ba2-004b-11e8-ba89-0ed5f89f718b"));
            } catch (IOException e) {
                e.printStackTrace();
            }
            this.bluetoothServerSocket = tmp;
        }

        public void run() {
            BluetoothSocket socket = null;
            // Keep listening until exception occurs or a socket is returned.
            while (true) {
                try {
                    socket = bluetoothServerSocket.accept();
                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }

                if (socket != null) {
                    try {
                        changeImage(receiveImage(socket));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    try {
                        bluetoothServerSocket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                }
            }
        }

        // Closes the connect socket and causes the thread to finish.
        public void cancel() {
            try {
                bluetoothServerSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }
}
