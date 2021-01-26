package fr.veim.sensortest;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;

public class FirstFragment extends Fragment implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor accelerometre;
    private Sensor gyroscope;
    private Sensor rotation;
    private PowerManager.WakeLock wakeLock;
    private final String WAKE_LOCK_TAG = "my:wakelock";
    private Gson gson;
    private OutputStreamWriter writer;

    private SensorsValues sensorsValues;

    private PowerManager powerManager;

    
    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        sensorManager = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
        powerManager = (PowerManager) getActivity().getSystemService(Context.POWER_SERVICE);
        sensorsValues = new SensorsValues();
        try {
            writer = new OutputStreamWriter(getContext().openFileOutput("result.txt", Context.MODE_PRIVATE));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        gson = new GsonBuilder().serializeSpecialFloatingPointValues().create();

        accelerometre = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        rotation = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);

        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG);

        wakeLock.acquire();

        registerListener();


        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_first, container, false);
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.findViewById(R.id.button_first).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                NavHostFragment.findNavController(FirstFragment.this)
                        .navigate(R.id.action_FirstFragment_to_SecondFragment);
            }
        });
    }

    private void registerListener () {
        sensorManager.registerListener(this, accelerometre, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, rotation, SensorManager.SENSOR_DELAY_NORMAL);
    }

    private void unregisterListener () {
        sensorManager.unregisterListener(this, accelerometre);
        sensorManager.unregisterListener(this, gyroscope);
        sensorManager.unregisterListener(this, rotation);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        sensorsValues.timestamp = event.timestamp;


        if (event.sensor == accelerometre) {
            // Convert to g from m/s²
            sensorsValues.acc_x = event.values[0] * 0.1019716213f;
            sensorsValues.acc_y = event.values[1] * 0.1019716213f;
            sensorsValues.acc_z = event.values[2] * 0.1019716213f;

            String json = gson.toJson(sensorsValues);
            try {
                writer.write(json);
                Log.d("JSON", json);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (event.sensor == gyroscope) {
            // Convert to °/s from rad/s
            sensorsValues.gyr_x = event.values[0] * 57.295779513f;
            sensorsValues.gyr_y = event.values[1] * 57.295779513f;
            sensorsValues.gyr_z = event.values[2] * 57.295779513f;

            String json = gson.toJson(sensorsValues);

            try {
                writer.write(json);
                Log.d("JSON", json);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (event.sensor == rotation) {
            // https://gist.github.com/cdflynn/16b49de79974ad211fa195c3e801488c
            float[] rotationMatrix = new float[9];
            float[] orientation = new float[3];
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values);
            SensorManager.getOrientation(rotationMatrix, orientation);
            sensorsValues.yaw =  orientation[0];
            sensorsValues.pitch = orientation[1];
            sensorsValues.roll = orientation[2];

            if(Double.isNaN(sensorsValues.yaw)){
                sensorsValues.yaw = 0.0f;
            }

            if(Double.isNaN(sensorsValues.pitch)){
                sensorsValues.pitch = 0.0f;
            }

            if(Double.isNaN(sensorsValues.roll)){
                sensorsValues.roll = 0.0f;
            }

            String json = gson.toJson(sensorsValues);

            try {
                writer.write(json);
                Log.d("JSON", json);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (writer != null) {
            try {
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}