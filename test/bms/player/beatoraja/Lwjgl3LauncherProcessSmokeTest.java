package bms.player.beatoraja;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;

/** Opt-in JavaFX parent + real GLFW child smoke test, without loading user configuration. */
public final class Lwjgl3LauncherProcessSmokeTest {
    private static final AtomicReference<Throwable> failure = new AtomicReference<>();

    public static void main(String[] args) {
        Application.launch(Probe.class, args);
        if (failure.get() != null) throw new AssertionError("JavaFX/GLFW process launch", failure.get());
        System.out.println("Lwjgl3LauncherProcessSmokeTest passed (JavaFX parent + GLFW child)");
    }

    public static final class Probe extends Application {
        @Override public void start(Stage stage) throws Exception {
            if (MainLoader.getAvailableDisplayMode().length == 0 || MainLoader.getDesktopDisplayMode().width <= 0)
                throw new AssertionError("JavaFX display-mode query");
            stage.setTitle("beatoraja Launcher process smoke test");
            stage.setScene(new Scene(bms.player.beatoraja.launcher.VideoConfigurationViewTest.verify(), 640, 560));
            stage.show();
            new Thread(() -> {
                Process child = null;
                try {
                    var command = new ArrayList<>(DesktopLauncher.gameCommand(System.getProperty("java.home"),
                            System.getProperty("java.class.path"), System.getProperty("os.name"),
                            ManagementFactory.getRuntimeMXBean().getInputArguments()));
                    command.set(command.indexOf(DesktopLauncher.class.getName()), Lwjgl3StartupMonitorSmokeTest.class.getName());
                    child = new ProcessBuilder(command).inheritIO().start();
                    if (!child.waitFor(30, TimeUnit.SECONDS) || child.exitValue() != 0)
                        throw new AssertionError("GLFW child did not exit successfully");
                } catch (Throwable error) {
                    failure.set(error);
                } finally {
                    if (child != null && child.isAlive()) child.destroyForcibly();
                    Platform.runLater(() -> { stage.close(); Platform.exit(); });
                }
            }, "launcher-smoke-wait").start();
        }
    }
}
