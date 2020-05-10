package bms.player.beatoraja.result;

import bms.player.beatoraja.ir.IRChartData;
import bms.player.beatoraja.ir.IRConnection;

import java.awt.*;
import java.net.URI;

public enum MusicResultCommand {

    OPEN_RANKING_ON_IR {
        @Override
        public void execute(MusicResult result) {
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
    };

    public abstract void execute(MusicResult result);

}
