@echo off
echo ========================================
echo Starting Appointment Service
echo ========================================
echo.

cd /d D:\medibridgeProd\appointment_service_medibridge

echo Checking if service is already running...
tasklist /FI "IMAGENAME eq java.exe" 2>NUL | find /I "java.exe" >NUL
if %ERRORLEVEL% EQU 0 (
    echo WARNING: Java process is already running!
    echo Please stop it first before starting the service.
    pause
    exit /b 1
)

echo Starting service with Maven...
echo.
echo The service will start on port 8083
echo WebSocket endpoint: http://localhost:8083/api/v1/ws
echo.
echo Press Ctrl+C to stop the service
echo.

mvn spring-boot:run

pause
