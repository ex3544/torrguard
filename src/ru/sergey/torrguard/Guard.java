package ru.sergey.torrguard;

import android.content.ContentResolver;
import android.content.Context;
import android.os.SystemClock;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;

/**
 * Проверка и восстановление службы спец. возможностей TorrServe MatriX.
 *
 * Два сценария поломки (подтверждены на Xiaomi MiTV alita / Android 9):
 *  A) процесс TorrServe убит системой — настройка цела, служба не подключена;
 *  B) force-stop (выгрузка из «недавних») — прошивка стирает настройку.
 *
 * Лечение обоих одно: перезапись настройки off→on, после чего
 * AccessibilityManagerService сам заново привязывает (и запускает) службу.
 */
final class Guard {

    private static final String TAG = "TorrGuard";

    static final String TORR_PKG = "ru.yourok.torrserve";
    static final String TORR_COMPONENT =
            "ru.yourok.torrserve/ru.yourok.torrserve.server.local.services.GlobalTorrService";
    static final int TORR_PORT = 8090;

    private Guard() {}

    /** Возвращает true, если было выполнено восстановление. */
    static boolean check(Context ctx) {
        try {
            ContentResolver cr = ctx.getContentResolver();
            String cur = Settings.Secure.getString(
                    cr, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            boolean inSetting = cur != null && cur.contains(TORR_COMPONENT);
            boolean alive = torrAlive();

            if (inSetting && alive) {
                lastRestore = 0;
                return false;             // всё в порядке
            }
            // антитряска: после восстановления даем TorrServe до 8 сек на старт HTTP
            long now = SystemClock.elapsedRealtime();
            if (now - lastRestore < 8000) {
                return false;
            }
            lastRestore = now;
            restore(cr);
            Log.w(TAG, "restored: inSetting=" + inSetting + " alive=" + alive);
            return true;
        } catch (Throwable t) {
            Log.w(TAG, "check failed", t);
            return false;
        }
    }

    private static volatile long lastRestore = 0;

    /** Переписывает настройку off→on, сохраняя прочие включённые службы. */
    private static void restore(ContentResolver cr) {
        String cur = Settings.Secure.getString(
                cr, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);

        ArrayList<String> keep = new ArrayList<>();
        if (cur != null) {
            for (String s : cur.split(":")) {
                String t = s.trim();
                if (!t.isEmpty() && !t.startsWith(TORR_PKG + "/")) {
                    keep.add(t);
                }
            }
        }
        String others = TextUtils.join(":", keep);

        // off — без TorrServe
        Settings.Secure.putString(cr, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES, others);
        // on — с TorrServe: система привяжет службу заново
        String full = others.isEmpty() ? TORR_COMPONENT : others + ":" + TORR_COMPONENT;
        Settings.Secure.putString(cr, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES, full);
        Settings.Secure.putInt(cr, Settings.Secure.ACCESSIBILITY_ENABLED, 1);
    }

    /** Жив ли TorrServe: TCP-проба его веб-сервера на loopback. */
    private static boolean torrAlive() {
        Socket s = new Socket();
        try {
            s.connect(new InetSocketAddress("127.0.0.1", TORR_PORT), 700);
            s.close();
            return true;
        } catch (IOException e) {
            try { s.close(); } catch (IOException ignore) {}
            return false;
        }
    }
}
