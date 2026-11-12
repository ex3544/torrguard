package ru.sergey.torrguard;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

/**
 * Постоянное JobScheduler-задание: переживает перезагрузку на уровне системы
 * (не зависит от BOOT_COMPLETED, который MIUI блокирует для «stopped» пакетов).
 * Каждые 15 минут гарантирует, что основная служба сторожа жива.
 */
public class GuardJobService extends JobService {

    private static final String TAG = "TorrGuard";
    private static final int JOB_ID = 4242;

    static void schedule(Context ctx) {
        JobScheduler js = ctx.getSystemService(JobScheduler.class);
        if (js == null) {
            return;
        }
        JobInfo info = new JobInfo.Builder(JOB_ID,
                new ComponentName(ctx, GuardJobService.class))
                .setPeriodic(15 * 60 * 1000L)
                .setPersisted(true)
                .build();
        js.schedule(info);
        Log.i(TAG, "job scheduled");
    }

    @Override
    public boolean onStartJob(JobParameters params) {
        Log.i(TAG, "job fired");
        Intent svc = new Intent(this, TorrGuardService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(svc);
        } else {
            startService(svc);
        }
        jobFinished(params, false);
        return false;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return true; // перепланировать
    }
}
