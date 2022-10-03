package bms.player.beatoraja;

import java.util.concurrent.ConcurrentHashMap;

import com.badlogic.gdx.utils.Array;
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
	private ConcurrentHashMap<K, ResourceCacheElement<V>> resourceMap = new ConcurrentHashMap<K, ResourceCacheElement<V>> ();

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
		return resourceMap.containsKey(key);
	}

	/**
	 * 指定したキーに対応するリソースを取得する。リソースがプールにない場合はloadを呼び出して
	 * リソースを取得し、プールに登録した上でリソースを返す。
	 *
	 * @param key リソースのキー
	 * @return リソース。読めなかった場合はnullを返す
	 */
 	public V get(K key) {
 		ResourceCacheElement<V> ie = resourceMap.get(key);
		if(ie == null) {
			V resource = load(key);
			if(resource != null) {
				ie = new ResourceCacheElement<V>(resource);
				resourceMap.put(key, ie);
			}
		} else {
			ie.gen = 0;
		} 			
		
		return ie != null ? ie.resource : null;
	}
 	
 	private final Array<K> removes = new Array<K>();

 	/**
 	 * 世代数を進め、最大世代数を経過したリソースを開放する
 	 */
	public void disposeOld() {
		removes.clear();
		resourceMap.forEach((key, value) -> {
			if(value.gen == maxgen) {
				dispose(value.resource);
				removes.add(key);
			} else {
				value.gen++;
			}			
		});
		
		for(K key : removes) {
			resourceMap.remove(key);
		}
	}

	/**
	 * 現在のリソースの要素数を返す。
	 * @return リソースの
	 */
	public int size() {
		return resourceMap.size();
	}
	
	public void dispose() {
		resourceMap.forEach((key, value) -> {
			dispose(value.resource);			
		});
		resourceMap.clear();
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
		public final R resource;
		/**
		 * 世代
		 */
		public int gen;
		
		public ResourceCacheElement(R resource) {
			this.resource = resource;
		}
	}
}