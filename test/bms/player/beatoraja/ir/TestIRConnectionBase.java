package bms.player.beatoraja.ir;

public abstract class TestIRConnectionBase implements IRConnection {

	@Override
	public IRResponse<IRPlayerData> register(IRAccount account) {
		return null;
	}

	@Override
	public IRResponse<IRPlayerData> login(IRAccount account) {
		return null;
	}

	@Override
	public IRResponse<IRPlayerData[]> getRivals() {
		return null;
	}

	@Override
	public IRResponse<IRTableData[]> getTableDatas() {
		return null;
	}

	@Override
	public IRResponse<IRScoreData[]> getPlayData(IRPlayerData player, IRChartData chart) {
		return null;
	}

	@Override
	public IRResponse<IRScoreData[]> getCoursePlayData(IRPlayerData player, IRCourseData course) {
		return null;
	}

	@Override
	public IRResponse<Object> sendPlayData(IRChartData chart, IRScoreData score) {
		return null;
	}

	@Override
	public IRResponse<Object> sendCoursePlayData(IRCourseData course, IRScoreData score) {
		return null;
	}

	@Override
	public String getSongURL(IRChartData chart) {
		return null;
	}

	@Override
	public String getCourseURL(IRCourseData course) {
		return null;
	}

	@Override
	public String getPlayerURL(IRPlayerData player) {
		return null;
	}
}
