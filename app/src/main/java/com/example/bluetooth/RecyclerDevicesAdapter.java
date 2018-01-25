package com.example.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

/**
 * Created by paul on 23/01/18.
 */

public class RecyclerDevicesAdapter extends RecyclerView.Adapter<RecyclerDevicesAdapter.ViewHolder> {
    private List<BluetoothDevice> bluetoothDevices;
    private Integer itemLayout;
    private Context context;

    public RecyclerDevicesAdapter(List<BluetoothDevice> bluetoothDevices, Integer itemLayout, Context context) {
        this.bluetoothDevices = bluetoothDevices;
        this.itemLayout = itemLayout;
        this.context = context;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(itemLayout, parent, false);
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        // trouve l'objet par position
        BluetoothDevice device = bluetoothDevices.get(position);
        // mets les donnees dans les textview
        if(device == null) {
            holder.txtAddress.setText("NULL");
            holder.txtName.setText("NULL");
        } else {
            holder.txtName.setText(device.getName());
            holder.txtAddress.setText(device.getAddress());
        }
    }

    @Override
    public int getItemCount() {
        return this.bluetoothDevices.size();
    }

    @Nullable
    public BluetoothDevice get(int position) {
        if(position < bluetoothDevices.size()) {
            return bluetoothDevices.get(position);
        } else {
            return null;
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView txtName, txtAddress;
        public ImageView img;

        public ViewHolder(View v) {
            super(v);
            txtName = v.findViewById(R.id.txtName);
            txtAddress = v.findViewById(R.id.txtAddress);
            img = v.findViewById(R.id.img);
        }
    }
}