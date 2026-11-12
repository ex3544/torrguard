#!/bin/bash
# Установка TorrGuard на ТВ и полный тест обоих сценариев поломки
ADB="C:/Users/Sergey/newMem/tools/platform-tools/adb.exe"
APK="C:/Users/Sergey/newMem/torrguard/TorrGuard.apk"
export MSYS_NO_PATHCONV=1
set -e

echo "=== 1. Установка APK ==="
"$ADB" install -r "$APK"

echo "=== 2. Выдача WRITE_SECURE_SETTINGS ==="
"$ADB" shell pm grant ru.sergey.torrguard android.permission.WRITE_SECURE_SETTINGS

echo "=== 3. Запуск службы ==="
"$ADB" shell am start-foreground-service -n ru.sergey.torrguard/.TorrGuardService
sleep 3
"$ADB" shell dumpsys activity services ru.sergey.torrguard | grep -E "ServiceRecord|isForeground" | head -4

echo ""
echo "=== 4. ТЕСТ СЦЕНАРИЯ Б (force-stop стирает настройку) ==="
echo "--- до: ---"
"$ADB" shell settings get secure enabled_accessibility_services
"$ADB" shell am force-stop ru.yourok.torrserve
echo "--- force-stop сделан, ждем 5 сек реакцию сторожа ---"
sleep 5
echo "--- после: ---"
"$ADB" shell settings get secure enabled_accessibility_services
"$ADB" shell dumpsys accessibility | grep -oE "Service\[label=TorrServe MatriX[^]]*\]" || echo "NOT BOUND"
"$ADB" shell "ps -A 2>/dev/null | grep -c torrserve || true"

echo ""
echo "=== 5. HTTP-пульс TorrServe (через 8 сек) ==="
sleep 8
curl -s -m 3 -o /dev/null -w "TorrServe HTTP: %{http_code}\n" "http://192.168.0.106:8090/" || echo "нет ответа"

echo "=== ГОТОВО ==="
