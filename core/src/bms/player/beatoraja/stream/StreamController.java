package bms.player.beatoraja.stream;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.logging.Logger;

import bms.player.beatoraja.MessageRenderer;
import bms.player.beatoraja.select.MusicSelector;
import bms.player.beatoraja.stream.command.StreamCommand;
import bms.player.beatoraja.stream.command.StreamRequestCommand;

/**
 * beatoraja パイプで受け取った文字列処理
 */
public class StreamController {
    StreamCommand[] commands;
    BufferedReader pipeBuffer;
    Thread polling;
    boolean isActive = false;
    MusicSelector selector;
    MessageRenderer notifier;

    public StreamController(MusicSelector selector, MessageRenderer notifier) {
        this.selector = selector;
        this.notifier = notifier;
        commands = new StreamCommand[] { new StreamRequestCommand(this.selector, this.notifier) };
        try {
            pipeBuffer = new BufferedReader(new FileReader("\\\\.\\pipe\\beatoraja"));
            isActive = true;
        } catch (Exception e) {
            e.printStackTrace();
            dispose();
        }
    }

    public void run() {
        if (pipeBuffer == null) {
            return;
        }
        polling = new Thread(() -> {
            try {
                String line = null;
                while (!Thread.interrupted()) {
                    try {
                        line = pipeBuffer.readLine();
                        if (line == null) {
                            break;
                        }
                        Logger.getGlobal().info("受信:" + line);
                        execute(line);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        try {
            while (!pipeBuffer.ready());
        } catch (IOException e) {
            e.printStackTrace();
        }
        polling.start();
    }

    public void dispose() {
        if (polling != null) {
            try {
                polling.interrupt();
                polling = null;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (pipeBuffer != null) {
            try {
                pipeBuffer.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        for(int i = 0; i < commands.length; i++) {
            commands[i].dispose();
        }
        Logger.getGlobal().info("パイプリソース破棄完了");
    }

    private void execute(String line) {
        for(int i = 0; i < commands.length; i++) {
            String cmd = commands[i].COMMAND_STRING + " ";
            String[] splitLine = line.split(cmd);
            String data = splitLine.length == 2 ? splitLine[1] : "";
            commands[i].run(data);
        }
    }

}
