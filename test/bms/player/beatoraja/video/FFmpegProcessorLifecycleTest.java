package bms.player.beatoraja.video;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.concurrent.LinkedBlockingDeque;

import bms.player.beatoraja.song.SongResources;

/** Verifies that HALT is never lost even when the playback queue is full. */
public final class FFmpegProcessorLifecycleTest {
    public static void main(String[] args) throws Exception {
        var processor = new FFmpegProcessor(1);
        var worker = processor.new MovieSeekThread(SongResources.fromPath(Path.of("unused.mp4")));
        Field queueField = worker.getClass().getDeclaredField("commands");
        queueField.setAccessible(true);
        var queue = (LinkedBlockingDeque<?>) queueField.get(worker);
        for (int i = 0; i < 4; i++) worker.exec(FFmpegProcessor.Command.STOP);
        if (queue.remainingCapacity() != 0) throw new AssertionError("Test queue must be full");
        worker.exec(FFmpegProcessor.Command.HALT);
        Field haltField = worker.getClass().getDeclaredField("haltRequested");
        haltField.setAccessible(true);
        if (!haltField.getBoolean(worker) || !queue.isEmpty()) {
            throw new AssertionError("HALT must bypass and clear the full command queue");
        }
        worker.exec(FFmpegProcessor.Command.PLAY);
        if (!queue.isEmpty()) throw new AssertionError("Stopped workers must reject new playback commands");
        worker.start();
        worker.join(5000);
        if (worker.isAlive()) {
            throw new AssertionError("A halted worker must exit without opening the movie");
        }
        Field converter = worker.getClass().getDeclaredField("converter"); converter.setAccessible(true);
        if (converter.get(worker) != null) throw new AssertionError("Converter is released even when halted before open");
        processor.dispose();
        processor.dispose();
        System.out.println("FFmpegProcessorLifecycleTest passed");
    }
}
