package bms.table;

/**
 * BMSTableManagerの変更通知を受けるためのリスナーインターフェイス
 * 
 * @author exch
 */
public interface BMSTableManagerListener {

	public abstract void modelChanged();
}
