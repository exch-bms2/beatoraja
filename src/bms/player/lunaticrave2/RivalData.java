package bms.player.lunaticrave2;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.LongProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * ライバルデータ
 * 
 * @author exch
 */
public class RivalData {
	
	/**
	 * アクティブライバルフラグ
	 */
	private BooleanProperty active = new SimpleBooleanProperty();
	/**
	 * プレイヤー名
	 */
	private StringProperty name = new SimpleStringProperty();
	/**
	 * プレイヤーID
	 */
	private StringProperty irid = new SimpleStringProperty();
	/**
	 * 段位
	 */
	private StringProperty rank = new SimpleStringProperty();
	/**
	 * プレイ曲数
	 */
	private IntegerProperty playbmscount = new SimpleIntegerProperty();
	/**
	 * プレイ回数
	 */
	private IntegerProperty playcount = new SimpleIntegerProperty();
	/**
	 * ライバルデータ取得日時
	 */
	private LongProperty date = new SimpleLongProperty();

	public String getIrid() {
		return irid.get();
	}

	public void setIrid(String irid) {
		this.irid.set(irid);
	}

	public StringProperty iridProperty() {
		return irid;
	}

	public int getPlaycount() {
		return playcount.get();
	}

	public void setPlaycount(int playcount) {
		this.playcount.set(playcount);
	}

	public IntegerProperty playcountProperty() {
		return playcount;
	}

	public int getSongcount() {
		return playbmscount.get();
	}

	public void setSongcount(int playbmscount) {
		this.playbmscount.set(playbmscount);
	}

	public IntegerProperty songcountProperty() {
		return playbmscount;
	}

	public String getName() {
		return name.get();
	}

	public void setName(String name) {
		this.name.set(name);
	}

	public StringProperty nameProperty() {
		return name;
	}

	public String getRank() {
		return rank.get();
	}

	public void setRank(String rank) {
		this.rank.set(rank);
	}

	public StringProperty rankProperty() {
		return rank;
	}

	public int getStatus() {
		return active.get() ? 1 : 0;
	}

	public void setStatus(int active) {
		this.active.set(active == 1);
	}

	public boolean getActive() {
		return active.get();
	}

	public void setActive(boolean active) {
		this.active.set(active);
	}

	public BooleanProperty activeProperty() {
		return active;
	}

	public long getDate() {
		return date.get();
	}

	public void setDate(long date) {
		this.date.set(date);
	}

	public LongProperty dateProperty() {
		return date;
	}


}