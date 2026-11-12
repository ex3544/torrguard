# TorrGuard — сторож TorrServe MatriX для Xiaomi Mi TV (alita)

> **EN TL;DR:** A tiny (12 KB) ADB-sideloaded watchdog for Xiaomi Mi TV (Android 9 / MIUI TV) that
> keeps the TorrServe MatriX accessibility service alive. It instantly restores the service when
> MIUI wipes the accessibility setting on force-stop or kills the background process — no more
> "enable TorrServe MatriX in Accessibility settings" dialog before every torrent stream.
> Builds without Gradle (aapt2 + javac + d8). See `install-test.sh` for install commands.

**Версия:** 1.2 (versionCode 3) · **Пакет:** `ru.sergey.torrguard` · **Размер:** 12,7 КБ

## Что делает

Самоубийство TorrServe больше не проблема. Два сценария поломки (подтверждены на этой прошивке Android 9 / MIUI TV):

- **А:** MIUI/LMK убивает процесс TorrServe — галка включена, но служба мертва → NUM показывает диалог;
- **Б:** force-stop (выгрузка из «запущенных») стирает настройку спец. возможностей → галка снята.

TorrGuard ловит оба: ContentObserver реагирует на стирание настройки мгновенно, опрос живости (TCP-проба 127.0.0.1:8090) — каждые 10 сек. Лечение — перезапись настройки off→on (эквивалент ручного переключения рычажка), после чего система сама переподключает и **запускает** TorrServe.

## Состав

- `src/ru/sergey/torrguard/` — 3 класса (служба, BootReceiver, логика Guard)
- `AndroidManifest.xml` — без активности (невидим в лаунчере и «недавних»)
- `build.sh` — сборка без Gradle (aapt2 + javac + d8 + zipalign + apksigner)
- `install-test.sh` — установка + выдача прав + автотест force-stop
- `TorrGuard.apk` — собранный APK
- **`torrguard.keystore`** — ⚠️ подпись. В репозиторий **не входит** (безопасность): хранится только локально у автора. Без него не выйдет обновить установленное приложение — беречь и не терять.

## Установка (если ТВ сброшен)

```bash
adb connect 192.168.0.106:5555
adb install -r TorrGuard.apk
adb shell pm grant ru.sergey.torrguard android.permission.WRITE_SECURE_SETTINGS
adb shell am start-foreground-service -n ru.sergey.torrguard/.TorrGuardService
```
(после перезагрузки стартует сам через BOOT_COMPLETED)

## Инструменты сборки

- JDK 17: `tools/jdk/`, build-tools 34: `tools/build-tools/android-14/`, платформа API 28: `tools/platform-28/android-9/`
- Пересборка: `bash torrguard/build.sh` (не забыть поднять version-code!)

## Ограничения

- Если сменить порт TorrServe с 8090 — поправить `Guard.TORR_PORT` и пересобрать.
- Если TorrServe удалить совсем — сторож будет раз в 8 сек пытаться его «включить» (безопасно, впустую). Отключение: `adb shell am force-stop ru.sergey.torrguard` + `adb uninstall ru.sergey.torrguard`.
