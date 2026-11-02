#!/bin/bash
cd "$(dirname "$0")"

# === Java Settings ===

# beatoraja 0.8.8+ requires JRE 17+ and JavaFX!
# Installing JRE/JDK with JavaFX included is recommended (e.g., Liberica JRE Full). Since Java 11, JavaFX is no longer bundled with most JDKs.

# Uncomment lines below if you use JRE/JDK not on $PATH. Set JRE_PATH to the folder that directly contains "bin/java".
# JRE_PATH="<path to JRE/JDK folder>"
# export JAVA_HOME="$(cd "$JRE_PATH" && pwd -P)"
# export PATH="$JAVA_HOME/bin:$PATH"

# Uncomment lines below if you use a separate JavaFX installation. This is only needed if your JRE/JDK does not include JavaFX. Set JAVAFX_PATH to the folder that directly contains "lib".
# JAVAFX_PATH="<path to JavaFX folder>"
# export JDK_JAVA_OPTIONS="${JDK_JAVA_OPTIONS:+$JDK_JAVA_OPTIONS }--module-path \"$(cd "$JAVAFX_PATH" && pwd -P)/lib\" --add-modules=javafx.controls,javafx.fxml"

# ======

# Check if Java and JavaFX exist
if ! java --describe-module javafx.controls &>/dev/null; then
  echo "[beatoraja-config.command] Java or JavaFX was not found, or Java is too old."
  echo "[beatoraja-config.command] Please make sure JRE 17+ with JavaFX is installed and configured correctly."
  echo
  echo "[beatoraja-config.command] Result of 'java -version':"
  java -version
  echo
  echo "[beatoraja-config.command] Result of 'java --describe-module javafx.controls':"
  java --describe-module javafx.controls
  echo
  read -p "Press Enter to exit..."
  exit 1
fi

# Run beatoraja
export JDK_JAVA_OPTIONS="${JDK_JAVA_OPTIONS:+$JDK_JAVA_OPTIONS }-Dfile.encoding=UTF-8"
exec java \
  -Xms1g -Xmx4g -XX:+UseZGC \
  -Djava.library.path=./natives -cp "beatoraja.jar:ir/*" bms.player.beatoraja.MainLoader