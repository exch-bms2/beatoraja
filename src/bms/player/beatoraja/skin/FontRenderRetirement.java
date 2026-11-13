package bms.player.beatoraja.skin;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Disposable;
import java.util.ArrayDeque;
import java.util.logging.Logger;
import java.util.IdentityHashMap;
import java.util.List;

/** 現フレームで送ったバッチが終わった後にネイティブのフォント資源を解放する。 / Native font cleanup after the current frame's submitted batches have ended. */
public final class FontRenderRetirement {
    private static final ArrayDeque<Disposable> PENDING = new ArrayDeque<>();
    private static boolean scheduled;
    private FontRenderRetirement() { }

    static synchronized void retire(Disposable resource) {
        PENDING.add(resource);
        if (Gdx.app != null && !scheduled) {
            scheduled = true;
            try {
                // Application の runnable は、現在の render() とウィンドウ更新が終わった後に実行される。
                // Application runnables execute after the current render() and window update have finished.
                Gdx.app.postRunnable(FontRenderRetirement::drain);
            } catch (RuntimeException failure) {
                scheduled = false;
                Logger.getGlobal().warning("Font cleanup retained for end-of-frame/shutdown drain: " + failure);
            }
        }
    }

    /**
     * すべての描画バッチ・ステージが終わった後、または終了処理の所有者がそれらを解放した後にだけ呼ぶ。
     * Only call after all render batches/stages have ended, or after their shutdown owners have released them.
     */
    public static void drainAfterBatch(SpriteBatch batch) {
        if (batch != null && batch.isDrawing()) throw new IllegalStateException("Font cleanup before batch end");
        if (batch == null && Gdx.gl != null) throw new IllegalStateException("GL cleanup requires the renderer boundary");
        drain();
    }

    /**
     * 終了処理の最後に一度だけ呼ぶ。共有アトラスと共通代替フォントを手放してから解放を実行する。
     * Call once at the end of shutdown: releases the shared atlases and the global fallback source, then drains.
     */
    public static void drainAtShutdown(SpriteBatch batch) {
        try {
            SystemFallbackGlyphs.shutdown();
        } finally {
            drainAfterBatch(batch);
        }
    }

    /**
     * 次に確認できた描画の区切りを待つ解放処理の数(ネイティブ確保数ではない)。
     * Number of cleanup actions awaiting the next verified renderer boundary (not a native allocation count).
     */
    static synchronized int pendingCount() { return PENDING.size(); }

    private static synchronized void drain() {
        RuntimeException failure = null;
        while (!PENDING.isEmpty()) {
            Disposable resource = PENDING.removeFirst(); // 先に外す。失敗しても再試行しない / detach first: failures are never retried
            try { resource.dispose(); }
            catch (RuntimeException cause) {
                if (failure == null) failure = new IllegalStateException("Font retirement failed");
                failure.addSuppressed(cause);
            }
        }
        scheduled = false;
        if (failure != null) throw failure;
    }

    /**
     * 所有するフォント資源の解放。借りている資源は渡してはいけない。
     * Cleanup for owned font resources; borrowed resources must never be passed here.
     */
    static final class FontResourceDisposer {

    	private FontResourceDisposer() {
    	}

    	/**
    	 * 所有資源を同一性ごとに一度ずつ解放し、実行時例外はまとめて最後に IllegalStateException として報告する。
    	 * 失敗したネイティブ解放は再試行しない。呼び出し前に所有者が参照を切り離しておくこと。
    	 * Attempts every owned resource once by identity and reports all runtime failures afterwards.
    	 * A failed native disposal is not retried. The owner must detach its references before calling.
    	 */
    	static void disposeAll(Disposable[] fallbacks, Disposable primary) {
    		var attempted = new IdentityHashMap<Disposable, Boolean>();
    		RuntimeException failure = null;
    		for (var resource : fallbacks) {
    			failure = disposeOne(resource, attempted, failure);
    		}
    		failure = disposeOne(primary, attempted, failure);
    		if (failure != null) {
    			throw failure;
    		}
    	}

    	/**
    	 * 一覧で渡された所有資源を {@link #disposeAll(Disposable[], Disposable)} と同じ規則で解放する。
    	 * Disposes a list of owned resources under the same rule as {@link #disposeAll(Disposable[], Disposable)}.
    	 */
    	static void disposeAll(List<Disposable> resources) {
    		disposeAll(resources.toArray(new Disposable[0]), null);
    	}

    	private static RuntimeException disposeOne(Disposable resource,
    			IdentityHashMap<Disposable, Boolean> attempted, RuntimeException failure) {
    		if (resource == null || attempted.put(resource, Boolean.TRUE) != null) {
    			return failure;
    		}
    		try {
    			resource.dispose();
    		} catch (RuntimeException cause) {
    			if (failure == null) {
    				failure = new IllegalStateException("Owned font resource cleanup failed");
    			}
    			failure.addSuppressed(cause);
    		}
    		return failure;
    	}
    }
}
