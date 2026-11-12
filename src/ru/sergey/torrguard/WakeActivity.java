package ru.sergey.torrguard;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

/**
 * Невидимая активность-будильник: запускается с пульта или adb
 * (am start -n ru.sergey.torrguard/.WakeActivity), поднимает службу
 * и снимает со пакета stopped-состояние (после adb install / чистильщика MIUI).
 */
public class WakeActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent svc = new Intent(this, TorrGuardService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(svc);
        } else {
            startService(svc);
        }
        GuardJobService.schedule(this);
        finish();
    }
}
