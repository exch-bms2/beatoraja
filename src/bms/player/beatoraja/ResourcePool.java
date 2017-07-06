package bms.player.beatoraja;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.badlogic.gdx.utils.Disposable;

/**
 * リソースプール。イメージデータやオーディオデータ等の読み込みコストが大きく、
 * なおかつ明示的な解放が必要なリソースをプールする仕組みを提供する。
 * 
 * @author exch
 *
 * @param <K> リソースを取り出すためのキー
 * @param <V> リソース
 */
public abstract class ResourcePool<K, V> implements Disposable {
	/**
	 * リソースの最大世代数
	 */
	private final int maxgen;
	/**
	 * リソース
	 */
	private Map<K, ResourceCacheElement<V>> image = new HashMap<K, ResourceCacheElement<V>> ();

	public ResourcePool(int maxgen) {
		this.maxgen = maxgen;
	}

	/**
	 * 指定するキーの要素が存在する場合はtrueを返す
	 *
	 * @param key リソースのキー
	 * @return キーに対応するリソースが存在する場合はtrue
	 */
	public boolean exists(K key) {
		return image.get(key) != null;
	}

	/**
	 * 指定したキーに対応するリソースを取得する。リソースがプールにない場合はloadを呼び出して
	 * リソースを取得し、プールに登録した上でリソースを返す。
	 *
	 * @param key リソースのキー
	 * @return リソース。読めなかった場合はnullを返す
	 */
 	public V get(K key) {
		ResourceCacheElement<V> ie = image.get(key);
		if(ie == null) {
			V resource = load(key);
			if(resource != null) {
				ie = new ResourceCacheElement<V>(resource);
				image.put(key, ie);
			}
		} else {
			ie.gen = 0;
		}
		
		return ie != null ? ie.image : null;
	}

 	/**
 	 * 世代数を進め、最大世代数を経過したリソースを開放する
 	 */
	public void disposeOld() {
		for(Entry<K, ResourceCacheElement<V>> entry : image.entrySet().toArray(new Entry[image.size()])) {
			ResourceCacheElement<V> ie = entry.getValue();
			if(ie.gen == maxgen) {
				dispose(ie.image);
				image.remove(entry.getKey());
			} else {
				ie.gen++;
			}
		}
	}

	/**
	 * 現在のリソースの要素数を返す。
	 * @return リソースの
	 */
	public int size() {
		return image.size();
	}
	
	public void dispose() {
		for(Map.Entry<K, ResourceCacheElement<V>> e : image.entrySet()) {
			dispose(e.getValue().image);
		}
		image.clear();
	}
	
	/**
	 * リソースを読み込む
	 * @param key リソースのキー
	 * @return リソース。読めなかった場合はnullを返す
	 */
	protected abstract V load(K key);

	/**
	 * リソースを開放する
	 * @param resource 開放するリソース
	 */
	protected abstract void dispose(V resource);

	/**
	 * リソース
	 *
	 * @param <R>
	 */
	private static class ResourceCacheElement<R> {
		/**
		 * リソース
		 */
		public R image;
		/**
		 * 世代
		 */
		public int gen;
		
		public ResourceCacheElement(R image) {
			this.image = image;
		}
	}
}