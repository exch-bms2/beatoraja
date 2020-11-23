package bms.player.beatoraja.result;

import bms.player.beatoraja.ir.IRChartData;
import bms.player.beatoraja.ir.IRConnection;
import bms.player.beatoraja.song.SongData;

import java.awt.*;
import java.net.URI;

public enum MusicResultCommand {

    OPEN_RANKING_ON_IR {
        @Override
        public void execute(MusicResult result, boolean next) {
            IRConnection ir = result.main.getIRStatus().length > 0 ? result.main.getIRStatus()[0].connection : null;
            if(ir == null) {
                return;
            }

            String url = ir.getSongURL(new IRChartData(result.main.getPlayerResource().getSongdata()));
            if (url != null) {
                try {
                    URI uri = new URI(url);
                    Desktop.getDesktop().browse(uri);
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
        }
    },
    /**
     * 譜面FAVORITE属性の変更
     */
    CHANGE_FAVORITE_CHART {
        @Override
        public void execute(MusicResult result, boolean next) {        
			final SongData sd = result.main.getPlayerResource().getSongdata();

			if(sd != null) {
				int type = 0;
				if((sd.getFavorite() & SongData.INVISIBLE_CHART) != 0) {
					type = 2;
				} else if((sd.getFavorite() & SongData.FAVORITE_CHART) != 0) {
					type = 1;
				}				
				type = (type + (next ? 1 : 2)) % 3;
				int favorite = sd.getFavorite();
				switch (type) {
				case 0:
					favorite &= 0xffffffff ^ (SongData.FAVORITE_CHART | SongData.INVISIBLE_CHART);
					break;
				case 1:
					favorite |= SongData.FAVORITE_CHART;
					favorite &= 0xffffffff ^ SongData.INVISIBLE_CHART;
					break;
				case 2:
					favorite |= SongData.INVISIBLE_CHART;
					favorite &= 0xffffffff ^ SongData.FAVORITE_CHART;
					break;
				}
				sd.setFavorite(favorite);
				result.main.getSongDatabase().setSongDatas(new SongData[]{sd});
			}
        }
    },
    /**
     * 次の楽曲FAVORITE属性の変更
     */
    CHANGE_FAVORITE_SONG {
        @Override
        public void execute(MusicResult result, boolean next) {        
			final SongData sd = result.main.getPlayerResource().getSongdata();

			if(sd != null) {
				int type = 0;
				if((sd.getFavorite() & SongData.INVISIBLE_SONG) != 0) {
					type = 2;
				} else if((sd.getFavorite() & SongData.FAVORITE_SONG) != 0) {
					type = 1;
				}				
				type = (type + (next ? 1 : 2)) % 3;
				int favorite = sd.getFavorite();
				switch (type) {
				case 0:
					favorite &= 0xffffffff ^ (SongData.FAVORITE_SONG | SongData.INVISIBLE_SONG);
					break;
				case 1:
					favorite |= SongData.FAVORITE_SONG;
					favorite &= 0xffffffff ^ SongData.INVISIBLE_SONG;
					break;
				case 2:
					favorite |= SongData.INVISIBLE_SONG;
					favorite &= 0xffffffff ^ SongData.FAVORITE_SONG;
					break;
				}
				sd.setFavorite(favorite);
				result.main.getSongDatabase().setSongDatas(new SongData[]{sd});
			}
        }
    };

    public abstract void execute(MusicResult result, boolean next);

}
