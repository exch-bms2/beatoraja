package bms.player.beatoraja.ir;

import bms.player.beatoraja.CourseData;
import bms.player.beatoraja.CourseData.CourseDataConstraint;

/**
 * IR用コースデータ
 * 
 * @author exch
 */
public class IRCourseData {

    /**
     * コース名
     */
    public final String name;
    /**
     * 楽曲のハッシュ
     */
    public final IRChartData[] charts;
    /**
     * コースの制限
     */
    public final CourseDataConstraint[] constraint;
	
    public IRCourseData(CourseData course) {
    	this.name = course.getName();
    	this.charts = new IRChartData[course.getSong().length];
    	for(int i = 0;i < this.charts.length;i++) {
    		charts[i] = new IRChartData(course.getSong()[i]);
    	}
    	this.constraint = new CourseDataConstraint[course.getConstraint().length];
    	for(int i = 0;i < this.constraint.length;i++) {
    		constraint[i] =course.getConstraint()[i];
    	}    	
    }
}
