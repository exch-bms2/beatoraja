package bms.player.beatoraja;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.badlogic.gdx.utils.Disposable;

/**
 * リソースプール
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
	
	private Map<K, ResourceCacheElement<V>> image = new HashMap<K, ResourceCacheElement<V>> ();

	public ResourcePool(int maxgen) {
		this.maxgen = maxgen;
	}
	
 	public V get(K path) {
		ResourceCacheElement<V> ie = image.get(path);
		if(ie == null) {
			V resource = load(path);
			if(resource != null) {
				ie = new ResourceCacheElement<V>(resource);
				image.put(path, ie);
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
	 * @param path リソースのキー
	 * @return リソース。読めなかった場合はnullを返す
	 */
	protected abstract V load(K path);

	/**
	 * リソースを開放する
	 * @param resource 開放するリソース
	 */
	protected abstract void dispose(V resource);

	private static class ResourceCacheElement<R> {
		public R image;
		public int gen;
		
		public ResourceCacheElement(R image) {
			this.image = image;
		}
	}
}