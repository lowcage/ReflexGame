package com.example.reflexgame;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import android.Manifest;
import android.content.pm.PackageManager;
import androidx.core.content.ContextCompat;


public class ReminderReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return; // nincs engedély → kilépünk
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "game_notify_channel")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Ne feledd!")
                .setContentText("Gyere vissza és dönts rekordot a reflexjátékban!")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        NotificationManagerCompat manager = NotificationManagerCompat.from(context);
        manager.notify(1001, builder.build());
    }

}
