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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        Intent serviceIntent = new Intent(this, MyBackgroundService.class);
        serviceIntent.setAction(android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
        startForegroundService(serviceIntent);

        // Delay a bit to ensure the service starts properly (optional)
        new Handler().postDelayed(() -> {
            // Minimize the app
            moveTaskToBack(true); // Moves app to the background
        }, 500);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Intent stopIntent = new Intent(this, MyBackgroundService.class);
        stopService(stopIntent);

    }

    /**
     * A native method that is implemented by the 'simpleservercpp' native library,
     * which is packaged with this application.
     */


}