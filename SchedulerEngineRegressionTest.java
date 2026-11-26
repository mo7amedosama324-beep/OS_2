import java.util.Arrays;
import java.util.List;

public class SchedulerEngineRegressionTest {
    public static void main(String[] args) {
        testPreemptiveSjfReevaluatesAfterContextSwitch();
        testAcsTier1PreemptsTier2AfterContextSwitch();
        testContextSwitchWithoutArrivalsKeepsSelection();
        testZeroContextSwitchAddsNoSwitching();
        System.out.println("All SchedulerEngine regression tests passed.");
    }

    private static void testPreemptiveSjfReevaluatesAfterContextSwitch() {
        SchedulerEngine engine = new SchedulerEngine();
        SimulationResult result = engine.runPreemptiveSjf(Arrays.asList(
                new Process("A", 0, 8, 1),
                new Process("B", 1, 5, 1),
                new Process("C", 2, 1, 1)), 2);

        assertSegmentStartsAt(result, "C", 3);
        assertMetric(result, "C", 1, 2, 1);
        assertMetric(result, "A", 0, 20, 12);
        assertMetric(result, "B", 5, 10, 5);
        assertSegmentStartsAt(result, "CS", 1);
        assertCompleteTickLog(result);
    }

    private static void testAcsTier1PreemptsTier2AfterContextSwitch() {
        SchedulerEngine engine = new SchedulerEngine();
        SimulationResult result = engine.runAcs(Arrays.asList(
                new Process("A", 0, 5, 1),
                new Process("B", 1, 1, 1),
                new Process("C", 5, 1, 1)), 1, 2);

        assertSegmentStartsAt(result, "C", 6);
        assertMetric(result, "C", 1, 2, 1);
        assertMetric(result, "A", 0, 13, 8);
        assertMetric(result, "B", 2, 3, 2);
        assertSegmentStartsAt(result, "CS", 4);
        assertCompleteTickLog(result);
    }

    private static void testContextSwitchWithoutArrivalsKeepsSelection() {
        SchedulerEngine engine = new SchedulerEngine();
        SimulationResult sjf = engine.runPreemptiveSjf(Arrays.asList(
                new Process("A", 0, 5, 1),
                new Process("B", 1, 1, 1)), 2);
        assertSegmentStartsAt(sjf, "B", 3);

        SimulationResult acs = engine.runAcs(Arrays.asList(
                new Process("A", 0, 5, 1),
                new Process("B", 1, 1, 1)), 1, 2);
        assertSegmentStartsAt(acs, "A", 6);
        assertMetric(acs, "A", 0, 10, 5);
        assertMetric(acs, "B", 2, 3, 2);
    }

    private static void testZeroContextSwitchAddsNoSwitching() {
        SchedulerEngine engine = new SchedulerEngine();
        SimulationResult sjf = engine.runPreemptiveSjf(Arrays.asList(
                new Process("A", 0, 4, 1),
                new Process("B", 1, 1, 1)), 0);
        SimulationResult acs = engine.runAcs(Arrays.asList(
                new Process("A", 0, 2, 1),
                new Process("B", 1, 1, 1)), 1, 0);

        assertNoContextSwitch(sjf);
        assertNoContextSwitch(acs);
        assertSegmentStartsAt(sjf, "B", 1);
        assertSegmentStartsAt(acs, "B", 1);
        assertCompleteTickLog(sjf);
        assertCompleteTickLog(acs);
    }

    private static void assertSegmentStartsAt(SimulationResult result, String label, int start) {
        for (SimulationResult.Segment segment : result.getSegments()) {
            if (label.equals(segment.getLabel()) && segment.getStartTime() == start) {
                return;
            }
        }
        throw new AssertionError("Expected " + label + " to start at t=" + start
                + ", got " + describe(result.getSegments()));
    }

    private static void assertMetric(SimulationResult result, String processName,
                                    int responseTime, int turnaroundTime, int waitingTime) {
        for (SimulationResult.ProcessMetrics metric : result.getMetrics()) {
            if (processName.equals(metric.getProcessName())) {
                if (metric.getResponseTime() != responseTime
                        || metric.getTurnaroundTime() != turnaroundTime
                        || metric.getWaitingTime() != waitingTime) {
                    throw new AssertionError("Unexpected metrics for " + processName
                            + ": response=" + metric.getResponseTime()
                            + ", turnaround=" + metric.getTurnaroundTime()
                            + ", waiting=" + metric.getWaitingTime());
                }
                return;
            }
        }
        throw new AssertionError("Missing metrics for " + processName);
    }

    private static void assertNoContextSwitch(SimulationResult result) {
        for (SimulationResult.Segment segment : result.getSegments()) {
            if ("CS".equals(segment.getLabel())) {
                throw new AssertionError("CS=0 must not produce a context-switch segment.");
            }
        }
    }

    private static void assertCompleteTickLog(SimulationResult result) {
        int endTime = 0;
        for (SimulationResult.Segment segment : result.getSegments()) {
            endTime = Math.max(endTime, segment.getEndTime());
        }
        if (result.getLogLines().size() != endTime) {
            throw new AssertionError("Expected one log line per tick through t=" + endTime
                    + ", got " + result.getLogLines().size());
        }
        for (int time = 0; time < endTime; time++) {
            if (!result.getLogLines().get(time).startsWith("t=" + time + " |")) {
                throw new AssertionError("Missing or out-of-order log entry for t=" + time);
            }
        }
    }

    private static String describe(List<SimulationResult.Segment> segments) {
        StringBuilder description = new StringBuilder();
        for (SimulationResult.Segment segment : segments) {
            if (description.length() > 0) {
                description.append(", ");
            }
            description.append(segment.getLabel()).append('[')
                    .append(segment.getStartTime()).append('-')
                    .append(segment.getEndTime()).append(']');
        }
        return description.toString();
    }
}
