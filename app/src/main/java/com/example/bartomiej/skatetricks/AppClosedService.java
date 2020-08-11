package com.example.bartomiej.skatetricks;

import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

public class AppClosedService extends Service {

    private NotificationManager mNM;
    static int iterator = 0;

    // Unique Identification Number for the Notification.
    // We use it on Notification start, and to cancel it.
    private int NOTIFICATION = R.string.local_service_started;

    public AppClosedService() {
        Log.i("SkateTricks", "(Service) Service has been started");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        if (iterator == 0) { //ensure that onTaskRemoved will be called once
            iterator++;
            Log.i("SkateTricks", "App Successfully Closed");

            //stopAccelerometer();
            MainActivity.stopAccelerometer1();
            MainActivity.stopGyroscope1();

            Toast.makeText(getApplicationContext(), "Disconnected safely\nfrom MW Device", Toast.LENGTH_LONG).show();

            //stop service
            this.stopSelf();
        }
    }
}
