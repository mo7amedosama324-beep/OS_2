import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Immutable output of one scheduling run.
 */
public class SimulationResult {
    private final List<Segment> segments;
    private final List<ProcessMetrics> metrics;
    private final List<String> logLines;
    private final double averageTurnaroundTime;
    private final double averageWaitingTime;

    SimulationResult(List<Segment> segments, List<ProcessMetrics> metrics,
                     List<String> logLines) {
        this.segments = Collections.unmodifiableList(new ArrayList<Segment>(segments));
        this.metrics = Collections.unmodifiableList(new ArrayList<ProcessMetrics>(metrics));
        this.logLines = Collections.unmodifiableList(new ArrayList<String>(logLines));

        double turnaroundTotal = 0;
        double waitingTotal = 0;
        for (ProcessMetrics row : metrics) {
            turnaroundTotal += row.turnaroundTime;
            waitingTotal += row.waitingTime;
        }
        this.averageTurnaroundTime = metrics.isEmpty() ? 0 : turnaroundTotal / metrics.size();
        this.averageWaitingTime = metrics.isEmpty() ? 0 : waitingTotal / metrics.size();
    }

    public List<Segment> getSegments() {
        return segments;
    }

    public List<ProcessMetrics> getMetrics() {
        return metrics;
    }

    public List<String> getLogLines() {
        return logLines;
    }

    public double getAverageTurnaroundTime() {
        return averageTurnaroundTime;
    }

    public double getAverageWaitingTime() {
        return averageWaitingTime;
    }

    public static final class Segment {
        private final String label;
        private final int startTime;
        private final int endTime;

        Segment(String label, int startTime, int endTime) {
            this.label = label;
            this.startTime = startTime;
            this.endTime = endTime;
        }

        public String getLabel() {
            return label;
        }

        public int getStartTime() {
            return startTime;
        }

        public int getEndTime() {
            return endTime;
        }
    }

    public static final class ProcessMetrics {
        private final String processName;
        private final int arrivalTime;
        private final int burstTime;
        private final int finishTime;
        private final int turnaroundTime;
        private final int waitingTime;
        private final int responseTime;

        ProcessMetrics(String processName, int arrivalTime, int burstTime, int finishTime,
                       int turnaroundTime, int waitingTime, int responseTime) {
            this.processName = processName;
            this.arrivalTime = arrivalTime;
            this.burstTime = burstTime;
            this.finishTime = finishTime;
            this.turnaroundTime = turnaroundTime;
            this.waitingTime = waitingTime;
            this.responseTime = responseTime;
        }

        public String getProcessName() {
            return processName;
        }

        public int getArrivalTime() {
            return arrivalTime;
        }

        public int getBurstTime() {
            return burstTime;
        }

        public int getFinishTime() {
            return finishTime;
        }

        public int getTurnaroundTime() {
            return turnaroundTime;
        }

        public int getWaitingTime() {
            return waitingTime;
        }

        public int getResponseTime() {
            return responseTime;
        }
    }
}
