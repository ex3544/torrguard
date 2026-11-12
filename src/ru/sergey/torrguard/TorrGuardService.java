package ru.sergey.torrguard;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;

public class TorrGuardService extends Service {

    private static final String TAG = "TorrGuard";
    private static final String CHANNEL_ID = "torrguard";
    private static final int NOTIF_ID = 4242;
    private static final long POLL_MS = 10000;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private ContentObserver observer;
    private java.util.concurrent.ExecutorService executor;

    /** Проверка уходит в фоновый поток: сокет-проба запрещена в главном. */
    private void checkAsync() {
        if (executor == null) {
            executor = java.util.concurrent.Executors.newSingleThreadExecutor();
        }
        executor.execute(new Runnable() {
            @Override
            public void run() {
                Guard.check(TorrGuardService.this);
            }
        });
    }

    private final Runnable poll = new Runnable() {
        @Override
        public void run() {
            checkAsync();
            handler.postDelayed(this, POLL_MS);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        NotificationManager nm = getSystemService(NotificationManager.class);
        if (nm != null && nm.getNotificationChannel(CHANNEL_ID) == null) {
            nm.createNotificationChannel(new NotificationChannel(
                    CHANNEL_ID, "TorrGuard", NotificationManager.IMPORTANCE_MIN));
        }
        Notification n = new Notification.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_notify_sync_noanim)
                .setContentTitle("TorrGuard")
                .setContentText("Слежу за TorrServe MatriX")
                .setOngoing(true)
                .setPriority(Notification.PRIORITY_MIN)
                .build();
        startForeground(NOTIF_ID, n);

        // Мгновенная реакция на стирание/изменение настройки (сценарий Б)
        observer = new ContentObserver(handler) {
            @Override
            public void onChange(boolean selfChange) {
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        checkAsync();
                    }
                }, 400);
            }
        };
        getContentResolver().registerContentObserver(
                Settings.Secure.getUriFor(Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES),
                false, observer);

        // Периодический опрос живости (сценарий А)
        handler.post(poll);
        Log.i(TAG, "service started");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        if (observer != null) {
            getContentResolver().unregisterContentObserver(observer);
        }
        if (executor != null) {
            executor.shutdown();
        }
        Log.w(TAG, "service destroyed");
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
