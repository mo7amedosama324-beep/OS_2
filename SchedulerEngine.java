import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Discrete-tick implementations of preemptive SJF and the adaptive
 * two-tier container scheduler.
 */
public class SchedulerEngine {
    private static final int AGING_INTERVAL = 4;

    public SimulationResult runPreemptiveSjf(List<Process> definitions, int contextSwitch) {
        validate(definitions, 1, contextSwitch);
        List<Process> processes = prepare(definitions);
        List<SimulationResult.Segment> segments = new ArrayList<SimulationResult.Segment>();
        List<String> log = new ArrayList<String>();
        int time = 0;
        int finished = 0;
        Process lastProcess = null;

        while (finished < processes.size()) {
            Process selected = shortestReadyProcess(processes, time);
            if (selected == null) {
                appendTick(segments, "IDLE", time);
                log.add(tickLog(time, "IDLE", Collections.<Process>emptyList(),
                        Collections.<Tier2Entry>emptyList(), time));
                time++;
                lastProcess = null;
                continue;
            }

            if (lastProcess != null && lastProcess != selected && contextSwitch > 0) {
                for (int i = 0; i < contextSwitch; i++) {
                    appendTick(segments, "CS", time);
                    log.add(tickLog(time, "CS", Collections.<Process>emptyList(),
                            Collections.<Tier2Entry>emptyList(), time));
                    time++;
                }
            }

            if (selected.firstRunTime < 0) {
                selected.firstRunTime = time;
            }
            appendTick(segments, selected.getName(), time);
            log.add(tickLog(time, selected.getName(), Collections.<Process>emptyList(),
                    Collections.<Tier2Entry>emptyList(), time));
            selected.remainingTime--;
            time++;
            lastProcess = selected;
            if (selected.remainingTime == 0) {
                selected.finishTime = time;
                finished++;
            }
        }
        return createResult(processes, segments, log);
    }

    public SimulationResult runAcs(List<Process> definitions, int quantum, int contextSwitch) {
        validate(definitions, quantum, contextSwitch);
        List<Process> processes = prepare(definitions);
        List<Process> tier1 = new ArrayList<Process>();
        List<Tier2Entry> tier2 = new ArrayList<Tier2Entry>();
        List<SimulationResult.Segment> segments = new ArrayList<SimulationResult.Segment>();
        List<String> log = new ArrayList<String>();

        int time = 0;
        int finished = 0;
        int nextArrival = 0;
        int nextTier2Order = 0;
        Process lastProcess = null;

        while (finished < processes.size()) {
            nextArrival = addArrivals(processes, nextArrival, time, tier1);
            Process running;
            boolean runningTier1;
            int quantumUsed = 0;

            if (!tier1.isEmpty()) {
                running = tier1.remove(0);
                runningTier1 = true;
            } else if (!tier2.isEmpty()) {
                running = selectTier2(tier2, time);
                runningTier1 = false;
            } else {
                appendTick(segments, "IDLE", time);
                log.add(tickLog(time, "IDLE", tier1, tier2, time));
                time++;
                lastProcess = null;
                continue;
            }

            if (lastProcess != null && lastProcess != running && contextSwitch > 0) {
                for (int i = 0; i < contextSwitch; i++) {
                    nextArrival = addArrivals(processes, nextArrival, time, tier1);
                    appendTick(segments, "CS", time);
                    log.add(tickLog(time, "CS", tier1, tier2, time));
                    time++;
                }
            }

            // Tier 2 is non-preemptive; Tier 1 runs until completion or quantum expiry.
            while (running.remainingTime > 0
                    && (!runningTier1 || quantumUsed < quantum)) {
                nextArrival = addArrivals(processes, nextArrival, time, tier1);
                if (running.firstRunTime < 0) {
                    running.firstRunTime = time;
                }
                appendTick(segments, running.getName(), time);
                log.add(tickLog(time, running.getName(), tier1, tier2, time));
                running.remainingTime--;
                quantumUsed++;
                time++;
            }

            lastProcess = running;
            if (running.remainingTime == 0) {
                running.finishTime = time;
                finished++;
            } else {
                running.timeEnteredTier2 = time;
                tier2.add(new Tier2Entry(running, nextTier2Order++));
            }
        }
        return createResult(processes, segments, log);
    }

    private static List<Process> prepare(List<Process> definitions) {
        List<Process> processes = new ArrayList<Process>();
        for (int i = 0; i < definitions.size(); i++) {
            processes.add(definitions.get(i).forSimulation(i));
        }
        Collections.sort(processes, new Comparator<Process>() {
            @Override
            public int compare(Process left, Process right) {
                int byArrival = Integer.compare(left.getArrivalTime(), right.getArrivalTime());
                return byArrival != 0 ? byArrival
                        : Integer.compare(left.getInputOrder(), right.getInputOrder());
            }
        });
        return processes;
    }

    private static void validate(List<Process> processes, int quantum, int contextSwitch) {
        if (processes == null || processes.isEmpty()) {
            throw new IllegalArgumentException("Add at least one process before running a simulation.");
        }
        if (quantum <= 0) {
            throw new IllegalArgumentException("Quantum must be greater than zero.");
        }
        if (contextSwitch < 0) {
            throw new IllegalArgumentException("Context switch time cannot be negative.");
        }
        for (Process process : processes) {
            if (process == null || process.getName().trim().isEmpty()) {
                throw new IllegalArgumentException("Every process must have a name.");
            }
            if (process.getArrivalTime() < 0) {
                throw new IllegalArgumentException("Arrival times cannot be negative.");
            }
            if (process.getBurstTime() <= 0) {
                throw new IllegalArgumentException("Burst times must be greater than zero.");
            }
            if (process.getPriority() < 1) {
                throw new IllegalArgumentException("Priorities must be at least 1.");
            }
        }
    }

    private static Process shortestReadyProcess(List<Process> processes, int time) {
        Process best = null;
        for (Process process : processes) {
            if (process.getArrivalTime() <= time && process.remainingTime > 0
                    && (best == null || process.remainingTime < best.remainingTime
                    || (process.remainingTime == best.remainingTime
                    && (process.getArrivalTime() < best.getArrivalTime()
                    || (process.getArrivalTime() == best.getArrivalTime()
                    && process.getInputOrder() < best.getInputOrder()))))) {
                best = process;
            }
        }
        return best;
    }

    private static int addArrivals(List<Process> processes, int nextArrival, int time,
                                   List<Process> tier1) {
        while (nextArrival < processes.size()
                && processes.get(nextArrival).getArrivalTime() <= time) {
            tier1.add(processes.get(nextArrival));
            nextArrival++;
        }
        return nextArrival;
    }

    private static Process selectTier2(List<Tier2Entry> waiting, final int time) {
        Tier2Entry best = Collections.min(waiting, new Comparator<Tier2Entry>() {
            @Override
            public int compare(Tier2Entry left, Tier2Entry right) {
                int byPriority = Integer.compare(effectivePriority(left, time),
                        effectivePriority(right, time));
                if (byPriority != 0) {
                    return byPriority;
                }
                int byArrival = Integer.compare(left.process.getArrivalTime(),
                        right.process.getArrivalTime());
                return byArrival != 0 ? byArrival
                        : Integer.compare(left.queueOrder, right.queueOrder);
            }
        });
        waiting.remove(best);
        return best.process;
    }

    private static int effectivePriority(Tier2Entry entry, int time) {
        int agingSteps = (time - entry.process.timeEnteredTier2) / AGING_INTERVAL;
        return Math.max(1, entry.process.getPriority() - agingSteps);
    }

    private static String tickLog(int time, String cpu, List<Process> tier1,
                                  List<Tier2Entry> tier2, int priorityTime) {
        return "t=" + time + " | CPU=" + cpu + " | T1=" + tier1State(tier1)
                + " | T2=" + tier2State(tier2, priorityTime);
    }

    private static String tier1State(List<Process> queue) {
        StringBuilder state = new StringBuilder("[");
        for (int i = 0; i < queue.size(); i++) {
            if (i > 0) {
                state.append(", ");
            }
            Process process = queue.get(i);
            state.append(process.getName()).append("(rem=").append(process.remainingTime)
                    .append(")");
        }
        return state.append("]").toString();
    }

    private static String tier2State(List<Tier2Entry> queue, int time) {
        StringBuilder state = new StringBuilder("[");
        for (int i = 0; i < queue.size(); i++) {
            if (i > 0) {
                state.append(", ");
            }
            Tier2Entry entry = queue.get(i);
            state.append(entry.process.getName()).append("(rem=")
                    .append(entry.process.remainingTime).append(", priority=")
                    .append(effectivePriority(entry, time)).append(")");
        }
        return state.append("]").toString();
    }

    private static void appendTick(List<SimulationResult.Segment> segments, String label,
                                   int time) {
        if (!segments.isEmpty()) {
            SimulationResult.Segment last = segments.get(segments.size() - 1);
            if (last.getLabel().equals(label) && last.getEndTime() == time) {
                segments.set(segments.size() - 1,
                        new SimulationResult.Segment(label, last.getStartTime(), time + 1));
                return;
            }
        }
        segments.add(new SimulationResult.Segment(label, time, time + 1));
    }

    private static SimulationResult createResult(List<Process> processes,
                                                List<SimulationResult.Segment> segments,
                                                List<String> log) {
        List<SimulationResult.ProcessMetrics> metrics =
                new ArrayList<SimulationResult.ProcessMetrics>();
        Collections.sort(processes, new Comparator<Process>() {
            @Override
            public int compare(Process left, Process right) {
                return Integer.compare(left.getInputOrder(), right.getInputOrder());
            }
        });
        for (Process process : processes) {
            int turnaround = process.finishTime - process.getArrivalTime();
            int waiting = turnaround - process.getBurstTime();
            int response = process.firstRunTime - process.getArrivalTime();
            metrics.add(new SimulationResult.ProcessMetrics(process.getName(),
                    process.getArrivalTime(), process.getBurstTime(), process.finishTime,
                    turnaround, waiting, response));
        }
        return new SimulationResult(segments, metrics, log);
    }

    private static final class Tier2Entry {
        private final Process process;
        private final int queueOrder;

        Tier2Entry(Process process, int queueOrder) {
            this.process = process;
            this.queueOrder = queueOrder;
        }
    }
}
