@echo off
echo ========================================
echo Restarting Appointment Service
echo ========================================
echo.

cd /d D:\medibridgeProd\appointment_service_medibridge

echo Checking for processes on port 8083...
for /f "tokens=5" %%a in ('netstat -ano ^| findstr :8083 ^| findstr LISTENING') do (
    echo Found process %%a, killing it...
    taskkill /PID %%a /F 2>nul
)

echo Waiting for port to be freed...
timeout /t 3 /nobreak >nul

echo.
echo Starting service on port 8083...
echo.
echo Watch for: "Started AppointmentServiceMedibridgeApplication"
echo.
echo Press Ctrl+C to stop the service
echo.

mvn spring-boot:run

pause
