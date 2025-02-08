package com.example.simpleservercpp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import com.example.simpleservercpp.databinding.ActivityMainBinding;

import android.os.Handler;
public class MainActivity extends AppCompatActivity {

    private BLEServer bleServer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize BLE Server
        bleServer = new BLEServer(this);

//        Intent serviceIntent = new Intent(this, MyBackgroundService.class);
//        startService(serviceIntent);

        // Delay a bit to ensure the service starts properly (optional)
        new Handler().postDelayed(() -> {
            // Minimize the app
            moveTaskToBack(true); // Moves app to the background
        }, 500);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (bleServer != null) {
//            bleServer.stopAdvertising();
            bleServer = null;
        }
    }

    /**
     * A native method that is implemented by the 'simpleservercpp' native library,
     * which is packaged with this application.
     */


}