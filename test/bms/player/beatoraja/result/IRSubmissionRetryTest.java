package bms.player.beatoraja.result;

import bms.player.beatoraja.*;
import bms.player.beatoraja.ir.*;
import bms.player.beatoraja.song.SongData;

/** No server: both result queues must honor unknown outcomes, including repeated send() calls. */
public final class IRSubmissionRetryTest {
    public static void main(String[] args) {
        for (boolean unknown : new boolean[]{false, true}) {
            int[] single = {0}, course = {0};
            var ir = new TestIRConnectionBase() {
                public IRResponse<Object> sendPlayData(IRChartData chart, IRScoreData score) { single[0]++; return response(unknown); }
                public IRResponse<Object> sendCoursePlayData(IRCourseData chart, IRScoreData score) { course[0]++; return response(unknown); }
            };
            var song = new SongData(); song.setTitle("fixture");
            var chart = new MusicResult.IRSendStatus(ir, song, new ScoreData());
            var data = new CourseData(); data.setName("fixture"); data.setSong(new SongData[]{song});
            var group = new CourseResult.IRSendStatus(ir, data, 0, new ScoreData());
            check(!chart.send() && !group.send(), "Submission fails");
            if (unknown) {
                check(chart.retry < 0 && group.retry < 0, "Remove ambiguous sends from both retry queues");
                chart.send(); group.send(); check(single[0] == 1 && course[0] == 1, "Never send an ambiguous status twice");
            } else check(chart.retry == 1 && group.retry == 1, "Legacy ordinary failure retry contract retained");
        }
        System.out.println("IRSubmissionRetryTest passed (chart/course duplicate prevention, legacy IRResponse compatibility)");
    }
    private static IRResponse<Object> response(boolean unknown) {
        if (unknown) return IRSubmissionResponse.unknown("Read timed out");
        return new IRResponse<>() {
            public boolean isSucceeded() { return false; }
            public String getMessage() { return "fixture"; }
            public Object getData() { return null; }
        };
    }
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
