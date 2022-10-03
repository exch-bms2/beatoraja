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
	/**
	 * トロフィーデータ
	 */
	public final IRTrophyData[] trophy;
	/**
	 * LN TYPE(-1 : 未指定, 0: LN, 1: CN, 2: HCN)
	 */
	public final int lntype;

    public IRCourseData(CourseData course) {
    	this(course, -1);
    }

    public IRCourseData(CourseData course, int lntype) {
    	this.name = course.getName();
    	this.charts = new IRChartData[course.getSong().length];
    	for(int i = 0;i < this.charts.length;i++) {
    		charts[i] = new IRChartData(course.getSong()[i]);
    	}
    	this.constraint = new CourseDataConstraint[course.getConstraint().length];
    	for(int i = 0;i < this.constraint.length;i++) {
    		constraint[i] =course.getConstraint()[i];
    	}
    	this.lntype = lntype;

    	this.trophy = new IRTrophyData[course.getTrophy().length];
    	for(int i = 0; i < trophy.length;i++) {
    		trophy[i] = new IRTrophyData(course.getTrophy()[i]);
		}
    }

	/**
	 * IR用トロフィーデータ
	 *
	 * @author exch
	 */
	public static class IRTrophyData {
		/**
		 * トロフィー名称
		 */
    	public final String name;
		/**
		 * トロフィーのスコアレート条件
		 */
		public final float scorerate;
		/**
		 * トロフィーのミスレート条件
		 */
		public final float smissrate;

    	public IRTrophyData (CourseData.TrophyData trophy) {
    		name = trophy.getName();
    		scorerate = trophy.getScorerate();
			smissrate = trophy.getMissrate();
		}
	}
}
