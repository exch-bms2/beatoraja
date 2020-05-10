package bms.player.beatoraja.ir;

import bms.player.beatoraja.song.SongData;

public class IRTableData {

    /**
     * 難易度表名
     */
    public final String name;

    public final IRTableFolder[] folders;

    public final IRCourseData[] courses;

    public IRTableData(String name, IRTableFolder[] folders, IRCourseData[] courses) {
        this.name = name;
        this.folders = folders;
        this.courses = courses;
    }

    public static class IRTableFolder {

        /**
         * フォルダ名
         */
        public final String name;
        /**
         * 譜面データ
         */
        public final IRChartData[] charts;

        public IRTableFolder(String name, IRChartData[] charts) {
            this.name = name;
            this.charts = charts;
        }
    }
}
