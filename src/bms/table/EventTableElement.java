package bms.table;

public class EventTableElement extends BMSTableElement {
	// TODO 楽曲入手状況のみが重要。スコアは不要かも
	// TODO 投票状況とか

	private String team;
	private String artist;

	public String getArtist() {
		return artist;
	}

	public void setArtist(String artist) {
		this.artist = artist;
	}

	public String getTeam() {
		return team;
	}

	public void setTeam(String team) {
		this.team = team;
	}
}
