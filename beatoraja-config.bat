@echo off
setlocal
cd /d "%~dp0"

:: === Java Settings ===

:: beatoraja 0.8.8+ requires JRE 17+ and JavaFX!
:: Installing JRE/JDK with JavaFX included is recommended (e.g., Liberica JRE Full). Since Java 11, JavaFX is no longer bundled with most JDKs.

:: Uncomment lines below if you use JRE/JDK not on %PATH%. Set JRE_PATH to the folder that directly contains "bin\java.exe".
:: set "JRE_PATH=<path to JRE/JDK folder>"
:: set "JAVA_HOME=%JRE_PATH%"
:: set "PATH=%JAVA_HOME%\bin;%PATH%"

:: Uncomment lines below if you use a separate JavaFX installation. This is only needed if your JRE/JDK does not include JavaFX. Set JAVAFX_PATH to the folder that directly contains "lib".
:: set "JAVAFX_PATH=<path to JavaFX folder>"
:: set "JDK_JAVA_OPTIONS=%JDK_JAVA_OPTIONS% --module-path "%JAVAFX_PATH%\lib" --add-modules=javafx.controls,javafx.fxml"

:: ======

:: Check if Java and JavaFX exist
java --describe-module javafx.controls >nul 2>&1
if errorlevel 1 (
    echo [beatoraja-config.bat] Java or JavaFX was not found, or Java is too old.
    echo [beatoraja-config.bat] Please make sure JRE 17+ with JavaFX is installed and configured correctly.
    echo.
    echo [beatoraja-config.bat] Result of 'java -version':
    java -version
    echo.
    echo [beatoraja-config.bat] Result of 'java --describe-module javafx.controls':
    java --describe-module javafx.controls
    echo.
    pause
    exit /b 1
)

:: Run beatoraja
set "JDK_JAVA_OPTIONS=%JDK_JAVA_OPTIONS% -Dfile.encoding=UTF-8"
java ^
    -Xms1g -Xmx4g -XX:+UseZGC ^
    -Djava.library.path=.\natives -cp "beatoraja.jar;ir/*" bms.player.beatoraja.MainLoader