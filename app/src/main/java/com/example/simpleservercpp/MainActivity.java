package com.example.simpleservercpp;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import com.example.simpleservercpp.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private BLEServer bleServer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Initialize UI components
        TextView connectionStatus = findViewById(R.id.connection_status);
        TextView clientState = findViewById(R.id.client_state);
        EditText receivedDataField = findViewById(R.id.received_data);
        EditText inputField = findViewById(R.id.input_field);
        Button sendButton = findViewById(R.id.send_button);
        Button advertisingButton = findViewById(R.id.advertising_button);
        // Initialize BLE Server
        bleServer = new BLEServer(this, connectionStatus, clientState, receivedDataField, inputField, sendButton, advertisingButton);

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