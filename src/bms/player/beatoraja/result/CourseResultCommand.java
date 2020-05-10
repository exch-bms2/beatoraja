package bms.player.beatoraja.result;

import bms.player.beatoraja.ir.IRConnection;
import bms.player.beatoraja.ir.IRCourseData;

import java.awt.*;
import java.net.URI;

public enum CourseResultCommand {

    OPEN_RANKING_ON_IR {
        @Override
        public void execute(CourseResult result) {
            IRConnection ir = result.main.getIRStatus().length > 0 ? result.main.getIRStatus()[0].connection : null;
            if(ir == null) {
                return;
            }

            String url = ir.getCourseURL(new IRCourseData(result.main.getPlayerResource().getCourseData()));
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

    public abstract void execute(CourseResult result);

}
