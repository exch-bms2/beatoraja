package bms.player.beatoraja.ir;

import bms.player.beatoraja.TableData;

/**
 * IR用譜面表データ
 * 
 * @author exch
 */
public class IRTableData {

    /**
     * 譜面表名
     */
    public final String name;
    /**
     * 譜面フォルダデータ
     */
    public final IRTableFolder[] folders;
    /**
     * コースデータ
     */
    public final IRCourseData[] courses;

    public IRTableData(String name, IRTableFolder[] folders, IRCourseData[] courses) {
        this.name = name;
        this.folders = folders;
        this.courses = courses;
    }
    
    public IRTableData(TableData table) {
    	this.name = table.getName();
    	this.folders = new IRTableFolder[table.getFolder().length];
    	for(int i = 0;i < folders.length;i++) {
    		final TableData.TableFolder tf = table.getFolder()[i];
    		IRChartData[] charts = new IRChartData[tf.getSong().length];
    		for(int j = 0;j < charts.length;j++) {
    			charts[j] = new IRChartData(tf.getSong()[j]);
    		}
    		folders[i] = new IRTableFolder(tf.getName(), charts);
    	}
    	this.courses = new IRCourseData[table.getCourse().length];
    	for(int i = 0;i < courses.length;i++) {
    		courses[i] = new IRCourseData(table.getCourse()[i]);
    	}
    }

    /**
     * 譜面フォルダデータ
     * 
     * @author exch
     */
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
