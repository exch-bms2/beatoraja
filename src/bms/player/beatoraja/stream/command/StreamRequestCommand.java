package bms.player.beatoraja.stream.command;

import com.badlogic.gdx.graphics.Color;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import bms.player.beatoraja.MessageRenderer;
import bms.player.beatoraja.select.MusicSelector;
import bms.player.beatoraja.select.bar.HashBar;
import bms.player.beatoraja.song.SongData;

/**
 * reqコマンドの処理
 */
public class StreamRequestCommand extends StreamCommand {
    MusicSelector selector;
    MessageRenderer notifier;
    int maxLength = 30;
    Thread updaterThread;
    UpdateBar updater;

    public StreamRequestCommand(MusicSelector selector, MessageRenderer notifier) {
        COMMAND_STRING = "!!req";
        this.selector = selector;
        this.notifier = notifier;
        maxLength = this.selector.main.getPlayerConfig().getMaxRequestCount();
        updater = new UpdateBar();
        updaterThread = new Thread(updater);
        updaterThread.start();
    }

    @Override
    public void run(String data) {
        if (data.length() != 64) {
            return;
        }

        // is sha256
        updater.set(data);
    }

    @Override
    public void dispose() {
        if (updaterThread != null) {
            updaterThread.interrupt();
        }
    }

    class UpdateBar implements Runnable {
		final int MESSAGE_TIME = 3000;

        HashBar bar;
        List<SongData> songDatas = new ArrayList<SongData>();

        // sha256 stack
        Stack<String> stack = new Stack<>();
        // lock obj
        private final Object lock = new Object();

        UpdateBar() {
            this.bar = new HashBar(selector, "Stream Request", new SongData[0]);
            this.bar.setSortable(false);
        }

        void set(String sha256) {
            synchronized (lock) {
                stack.add(sha256);
                addMessage(sha256);
            }
        }
		
        void addMessage(String sha256) {
            if (notifier != null) {
                SongData[] _songDatas = selector.getSongDatabase().getSongDatas(new String[] { escape(sha256) });
                if (_songDatas.length > 0) {
                    SongData data = _songDatas[0];
                    if (songDatas.stream().filter(song -> song.getSha256().equals(sha256)).count() > 0 ||
                        stack.stream().filter(hash -> hash.equals(sha256)).count() > 1) { // stackの中身には自身を含めるため、1個の場合は通す
                        // すでに追加済みならスキップ
                        notifier.addMessage(data.getFullTitle() + " はリクエスト済です" , MESSAGE_TIME, Color.ORANGE, 0);
                    }
                    notifier.addMessage("リクエスト追加: " + data.getFullTitle() , MESSAGE_TIME, Color.LIME, 0);
                } else {
                    notifier.addMessage("リクエストされた譜面を所持していません" , MESSAGE_TIME, Color.ORANGE, 0);
                }
            }
		}

        void update() {
            synchronized (lock) {
                // 選曲画面でないなら更新しない
                if (selector.main.getCurrentState() instanceof MusicSelector) {
                    // 溜まってるぶんを順に取得
                    while (stack.size() != 0) {
                        String sha256 = stack.pop();
                        if (songDatas.stream().filter(song -> song.getSha256().equals(sha256)).count() > 0) {
                            // すでに追加済みならスキップ
                            continue;
                        }
                        SongData[] _songDatas = selector.getSongDatabase().getSongDatas(new String[] { escape(sha256) });
                        if (_songDatas.length > 0) {
                            songDatas.add(_songDatas[0]);
                        }
                        if (songDatas.size() > maxLength) {
                            songDatas.remove(0);
                        }
                    }

                    if (songDatas.size() > 0) {
                        bar.setElements(songDatas.toArray(new SongData[0]));
                        try {
                            selector.getBarManager().setAppendDirectoryBar("Stream Request", bar);
                            selector.getBarManager().updateBar();
                        } catch (Exception e) {
                        } // continue
                    }
                }
            }
        }

        private String escape(String before) {
            // とりあえずSQLに渡すのでエスケープする
            StringBuilder after = new StringBuilder();
            for (int i = 0; i < before.length(); i++) {
                char c = before.charAt(i);
                if (c == '_' || c == '%' || c == '\\') {
                    after.append('\\');
                }
                after.append(c);
            }
            return after.toString();
        }

        @Override
        public void run() {
            while (true) {
                try {
                    if (stack.size() != 0) {
                        update();
                    }
                } catch (Exception e) {
                    break;
                }
            }
        }
    }
}
