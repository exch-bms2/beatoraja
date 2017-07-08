REM *** Set specific UTF-8 encoding for config files etc to UTF-8. Note this is important because sometimes .json files do NOT save correctly otherwise resulting in corrupted files! ***
set JAVA_TOOL_OPTIONS=-Dfile.encoding=UTF-8 
REM *** Set system-wide "_JAVA_OPTIONS" environment variable to use OpenGL pipeline (improved performance of > 30% potentially. Also use anti-aliasing for non-LR2 fonts, and finally allow Swing framework to utilize AA and GTKLookAndFeel for config window. ***
set _JAVA_OPTIONS='-Dsun.java2d.opengl=true -Dawt.useSystemAAFontSettings=on -Dswing.aatext=true -Dswing.defaultlaf=com.sun.java.swing.plaf.gtk.GTKLookAndFeel'
java -Xms1g -Xmx4g -jar beatoraja.jar -c
