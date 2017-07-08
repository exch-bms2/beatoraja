REM *** Set system-wide "_JAVA_OPTIONS" environment variable to use OpenGL pipeline (improved performance of > 30% potentially. Also use anti-aliasing for non-LR2 fonts, and finally allow Swing framework to utilize AA and GTKLookAndFeel for config window. ***
set _JAVA_OPTIONS='-Dsun.java2d.opengl=true -Dawt.useSystemAAFontSettings=on -Dswing.aatext=true -Dswing.defaultlaf=com.sun.java.swing.plaf.gtk.GTKLookAndFeel'
java -jar beatoraja.jar -c
