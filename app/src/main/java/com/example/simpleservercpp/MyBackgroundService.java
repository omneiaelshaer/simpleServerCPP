package com.example.simpleservercpp;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import java.security.Provider;

public class MyBackgroundService extends Service {
    private BLEServer bleServer;
    private boolean isRunning = true; // Control flag for loop
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //determine what a service will do
        // Initialize BLE Server
        bleServer = new BLEServer(this);
        new Thread(
                new Runnable() {
                    @Override
                    public void run() {
                        while(isRunning)
                        {
                            Log.d("BLEServer foreground", "Service is running");
                            try {
                                Thread.sleep(2000);
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }
                }
        ).start();

        final String CHANNELID = "Forground Service ID";
        NotificationChannel channel = new NotificationChannel(CHANNELID,CHANNELID,
                NotificationManager.IMPORTANCE_LOW);
        getSystemService(NotificationManager.class).createNotificationChannel(channel);
        Notification.Builder notification = new Notification.Builder(this, CHANNELID)
                .setContentTitle("BLE Server Running")
                .setContentText("The BLE server is running in the background.")
                .setSmallIcon(R.drawable.ic_launcher_foreground);

        startForeground(1001,notification.build());
        return super.onStartCommand(intent, flags, startId);
    }

    // Method to stop the service gracefully
    public void stopService() {
        Log.d("Bluetooth", "Stopping service...");
        isRunning = false; // Stop the loop
        stopForeground(true);
        stopSelf();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }



}
